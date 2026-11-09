# ADR-012: Métricas en un puerto aparte, con las consultas versionadas

- **Estado:** Aceptada
- **Fecha:** 2026-11-09

## Contexto

El sistema ya registraba todo lo que hacía: logs JSON por decisión, auditoría append-only en la base y la evaluación offline. Pero nada de eso contesta la pregunta de guardia: *¿ahora mismo está funcionando?*

Hay tres preguntas que la auditoría no responde porque no es su trabajo:

1. **¿El proveedor está respondiendo?** La auditoría guarda la latencia de cada decisión, pero consultarla exige una query y saber qué buscar. Si Jev se cae a las tres de la mañana, nadie va a escribir un SQL.
2. **¿Hay tickets quietos?** Un ticket que falla al rutear vuelve a quedar en `NUEVO` y se reintenta en la próxima pasada del worker. Si falla siempre, el reintento es silencioso: no hay error nuevo, solo un ticket que no avanza.
3. **¿El outbox está publicando?** La API responde bien, las asignaciones se guardan, y los eventos podrían llevar horas sin salir.

`docs/monitoring.md` ya definía qué mirar. Lo que faltaba era exponerlo.

## Decisión

### Actuator escucha en su propio puerto

`management.server.port` es 9091, separado del 8080 de la API.

Las métricas no llevan datos personales —eso está prohibido por RNF-04 y se respeta: lo que se publica son conteos, latencias y estados, nunca el texto de un ticket—. Pero sí dicen cuántos tickets hay sin atender, qué tan atrás va el outbox y en qué momento se cayó el proveedor. Eso es información de operación, y no tiene por qué ser alcanzable desde donde se atiende al público.

Con el puerto separado, Prometheus puede raspar sin credenciales: el puerto no se publica fuera de la red de monitoreo. La cadena de seguridad igual se escribe acotada a `/actuator/**` y no abierta del todo, para que unificar los puertos algún día no abra la API entera. Hay un test que verifica que `/actuator/health` **no** responde en el puerto de la API.

La alternativa era dejar todo en 8080 con `permitAll`. Es lo que hace mucha gente y es lo que estaba. Mover el puerto costó una propiedad y una cadena de seguridad.

### El caso de uso no importa Micrometer

El ruteo habla con un puerto, `RoutingMetrics`, y la implementación con Micrometer vive en el adapter. No es ceremonia: `RouteTicketUseCase` se prueba sin Spring, y la interfaz trae un `NONE` de tres métodos vacías que evita montar un registry para verificar una regla de negocio.

### Las métricas de negocio son gauges sobre la base, no contadores en memoria

`ops_review_overrides` y `ops_review_comparable` leen lo que hay guardado. Un contador de Micrometer arranca en cero cada vez que se reinicia el proceso, y una tasa de override que se resetea con cada deploy no sirve para nada.

El costo es una consulta por scrape. Con este volumen son conteos sobre índices. Si la tabla creciera, el próximo paso es una vista materializada, no un caché en memoria: el caché reintroduce el problema de que dos gauges del mismo scrape describan instantes distintos.

Las cinco cifras de la cola salen de **una sola** consulta por eso mismo. Un panel que muestre cero pendientes y a la vez una antigüedad del más viejo hace dudar del tablero entero, y con razón.

### La tasa de override no se publica como métrica

Es un cociente, y mientras no haya resoluciones comparables no vale cero: no vale nada. Prometheus no sabe expresar "sin dato", así que se publican el numerador y el denominador por separado y la división la hace el panel, que queda vacío cuando el divisor es cero.

Es la misma decisión que ya se había tomado en la API y en la consola, por el mismo motivo: un cero inventado se lee como que el motor nunca se equivoca.

### El circuit breaker se publica como una serie por estado

`ops_decision_circuit_state{state="open"}` vale 1 o 0, en vez de un único gauge con 0, 1, 2 codificando el estado. Un número codificado parece más corto hasta que hay que escribir la alerta sin recordar qué número era cuál, y el promedio entre dos instancias daría un estado que no existe.

### Paneles y alertas versionados en el repositorio

El tablero de Grafana se carga por provisioning desde `ops/grafana/dashboards/`, con `allowUiUpdates: false`. Un tablero editado a mano vive en el volumen del contenedor, se pierde con él y no se puede revisar en un diff.

Las consultas de las alertas son las mismas que las de los paneles. Un `PrometheusEndpointTest` fija los nombres de las series de las que cuelgan: si alguien renombra una métrica, falla el build y no un panel en silencio.

## Consecuencias

**Positivas**
- Las seis preguntas de `monitoring.md`, sección 4.1, tienen panel.
- Las alertas y los paneles se revisan en el pull request como cualquier otro código.
- Renombrar una métrica rompe el build en vez de vaciar un panel.

**Negativas**
- **Los umbrales son propuestos, no medidos.** Salen de `monitoring.md` y no de tráfico real, porque no hay tráfico real. Un p95 de 500 ms o un 30% de derivación son puntos de partida razonables, no valores calibrados. Está escrito en el archivo de alertas.
- **No hay trazas.** `monitoring.md` preveía Micrometer Tracing con OpenTelemetry y un `X-Request-Id` en el MDC. Quedó afuera: sin un backend de trazas y con un solo servicio, una traza por request no agrega nada sobre lo que ya dicen los logs.
- **Nadie recibe las alertas.** Prometheus las evalúa y las muestra, pero no hay Alertmanager ni destino. Para el objetivo del proyecto, que es mostrar de qué se alerta y con qué consulta, el destino es lo menos interesante.
- **Una consulta a la base por scrape.** Documentada arriba, con su salida si el volumen cambia.
- **El histograma de latencia HTTP tiene muchas series.** Habilitar `percentilesHistogram` publica unos cuarenta cubos por endpoint y por código de estado: con nueve endpoints son alrededor de 340 series solo para eso, sobre 880 en total. Es el precio de poder calcular un p95 del lado del servidor en vez de promediar. Si la cantidad de endpoints creciera, conviene reemplazarlo por cubos elegidos a mano alrededor del objetivo de latencia.
- **Grafana corre con acceso anónimo de administrador.** Es un entorno de demostración en localhost. Si esto sale de la máquina, esa línea del compose es lo primero que hay que sacar, y está comentada como tal.
- La aplicación ahora ocupa dos puertos. Hay que recordarlo al desplegarla.

## Alternativas consideradas

- **Solo logs, sin métricas:** los logs responden "qué pasó con este ticket", no "cómo viene el sistema". Agregar sobre logs para alertar es construir un sistema de métricas peor.
- **Dejar actuator en el puerto de la API con `permitAll`:** una propiedad menos y un dato de operación publicado al mundo.
- **Contadores en memoria para las cifras de negocio:** más baratos, pero se reinician con el proceso.
- **Alertmanager con un webhook:** mostraría una notificación llegando, pero el valor del proyecto está en la consulta que define la alerta, no en el canal.
- **Micrometer Tracing:** útil con varios servicios. Con uno solo, agrega dependencias y un colector para contar lo que ya cuenta el log.

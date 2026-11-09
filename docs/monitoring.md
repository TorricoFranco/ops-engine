# ops-engine: logs y monitoreo

## 1. Principio: cuatro cosas distintas

- **Logs:** qué pasó. Sirven para diagnosticar.
- **Métricas:** cuánto y qué tan rápido. Sirven para alertar y ver tendencias.
- **Trazas:** el recorrido de una request entre componentes, incluida la llamada a Jev.
- **Auditoría (en base de datos, no en logs):** qué decidió el sistema y por qué. Es un requisito de negocio y tiene acceso restringido. No se mezcla con los logs técnicos.

## 2. Stack propuesto

- **Spring Boot Actuator:** `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.
- **Micrometer + Prometheus + Grafana:** métricas y dashboards.
- **Logback con salida JSON** (logstash-logback-encoder) a stdout. Opcional: Loki o ELK para búsqueda.
- **Micrometer Tracing + OpenTelemetry:** una traza por request, con spans para la base de datos y para la llamada a Jev.
- **Correlation ID:** header `X-Request-Id`, cargado en MDC. Aparece en todos los logs y en la auditoría.

## 3. Qué loguear y qué no

**Campos en cada log:** timestamp, level, service, traceId, requestId, userId, ticketId, decisionId, ruleVersion.

| Nivel | Eventos |
|---|---|
| INFO | Ticket creado. Decisión tomada (tipo, resultado, duración en ms). Asignación realizada. Revisión resuelta. |
| WARN | Fallback a revisión. Reintento hacia Jev. Circuit breaker abierto o cerrado. Idempotency key repetida. Conflicto de optimistic locking. |
| ERROR | Excepción no controlada. Fallo de base de datos. Evento del outbox que agotó sus reintentos. |
| DEBUG | Solo en desarrollo. |

**Nunca loguear:**
- Contraseñas, tokens ni API keys (`TYPESAFE_API_KEY` incluida).
- El texto completo del ticket si puede contener datos personales.
- Prompts completos ni respuestas completas de la IA. Si hace falta guardarlas, van a la auditoría con acceso restringido, o como resumen.

## 4. Qué monitorear

### 4.1 Mínimo viable: seis paneles

1. Latencia p95 y tasa de errores 5xx de la API.
2. Llamadas a Jev: latencia p95, tasa de errores y timeouts, estado del circuit breaker.
3. Tasa de fallback a revisión: porcentaje de decisiones que terminan en `REVISION_REQUERIDA`.
4. Cola sin asignar: cantidad de tickets y antigüedad del más viejo.
5. Tickets con SLA en riesgo: vencen en menos de X minutos y todavía no están asignados o resueltos.
6. Tasa de override humano: porcentaje de veces que el operador cambia la decisión de la IA.

### 4.2 Completo, por categoría

**A. Técnico (señales doradas)**
- Tráfico (requests por segundo).
- Latencia p50, p95 y p99.
- Errores 4xx y 5xx.
- Saturación: heap, GC, threads, conexiones activas y en espera de HikariCP.

**B. Integración con IA**
- Requests a Jev por minuto.
- Latencia, errores, timeouts y reintentos.
- Estado del circuit breaker.
- Porcentaje de respuestas noul cercanas a 0,5, que indican incertidumbre.
- Distribución de confianzas por tipo de pregunta.
- Costo estimado por decisión.

**C. Negocio y ruteo**
- Tickets creados, asignados y revisados por hora.
- Carga por agente (tickets abiertos por persona) y desbalance entre el mayor y el menor.
- Tiempo medio de asignación y tiempo medio de resolución.
- Cumplimiento de SLA (porcentaje).
- Revisiones pendientes y su antigüedad.
- Acuerdo IA-humano, separado por tipo de ticket.

**D. Outbox y eventos**
- Eventos pendientes y antigüedad del más viejo.
- Reintentos fallidos.
- Eventos que pasaron a estado de error.

**E. Base de datos**
- Consultas lentas, locks y conexiones.
- Tamaño de la tabla de decisiones, que crece rápido.
- Duración de las migraciones.

**F. Seguridad**
- Logins fallidos por minuto.
- Respuestas 403 por rol.
- Cambios en reglas y permisos: quién y cuándo.

**G. Calidad (periódica, desde la evaluación offline)**
- Resultado de la última evaluación: precisión, tasa de revisión e impacto en SLA.
- Comparación entre versiones de reglas.

## 5. Alertas sugeridas

Los umbrales son propuestas. Se ajustan con datos reales.

| Alerta | Condición | Severidad |
|---|---|---|
| API lenta | p95 mayor a 500 ms durante 5 min | warning |
| Errores de la API | 5xx mayor al 1% durante 5 min | critical |
| Jev caído | Circuit breaker abierto más de 2 min | critical |
| Fallback alto | Más del 30% de decisiones a revisión en 15 min | warning |
| Cola atascada | Ticket sin asignar más de 15 min | warning |
| SLA en riesgo | Ticket con vencimiento en menos de 30 min y sin asignar | critical |
| Outbox atrasado | Evento pendiente más de 5 min | warning |
| Pool de base de datos | Conexiones en espera durante 5 min | warning |

## 6. Cómo se ve en el portfolio

- Un dashboard de Grafana con tres secciones: salud técnica, IA y negocio.
- Un test de integración que verifica que el log de una decisión incluye `traceId` y `decisionId`.
- Capturas del dashboard en el README.

## 7. Qué se implementó y qué no

Resuelto en la fase 7 (ADR-012). Prometheus y Grafana entraron al Docker Compose recién acá, no desde el inicio: antes de la fase 5 no había casi nada que medir, y dos contenedores que nadie mira solo hacen más lento el arranque.

**Implementado**
- Actuator en un puerto propio, el 9091, con `/actuator/prometheus` sin credenciales porque ese puerto no se publica.
- Los seis paneles de la sección 4.1, más la cola abierta por estado y el atraso del outbox.
- Las ocho alertas de la sección 5, en `ops/prometheus/alerts.yml`.
- Métricas propias: `ops_routing_decisions_total`, `ops_decision_judgement_seconds`, `ops_routing_failures_total`, `ops_decision_circuit_state`, `ops_tickets_*`, `ops_review_*` y `ops_outbox_pending`.

**No implementado, a conciencia**
- **Trazas.** Con un solo servicio, una traza por request no agrega sobre lo que ya dice el log.
- **Alertmanager.** Las alertas se evalúan y se ven, pero no se envían a ningún lado.
- **Loki o ELK.** Los logs siguen saliendo en JSON a stdout, sin backend de búsqueda.
- **Las métricas de las secciones 4.2 E, F y G.** Base de datos, seguridad y calidad. Las dos primeras salen casi gratis de los binders que ya trae Actuator; la de calidad depende de correr la evaluación periódicamente, que hoy es manual.

Los umbrales de la sección 5 siguen siendo propuestas. No hay tráfico real con el cual calibrarlos, y están así escritos en el archivo de alertas.

# Evaluación offline

Compara formas de rutear tickets sobre un mismo conjunto de casos etiquetados a mano, para
responder una pregunta concreta: **¿el motor le gana a una heurística simple, y cuánto?**

## Cómo correrla

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--evaluate"
```

No levanta Spring ni toca la base: es una simulación en memoria. Lee `dataset.csv` y escribe
`report.md` en esta misma carpeta.

Para usar otros archivos:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--evaluate --dataset mi.csv --report salida.md"
```

Por defecto el reporte dice que los tickets son sintéticos. Para un dataset de tickets reales,
declará el origen con `--origen reales`:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--evaluate --dataset mi.csv --origen reales"
```

El reporte también suma los tokens de entrada que factura Jev y estima el costo. Los de salida son
gratis. El precio por defecto es el publicado por TypeSafe, 0.042 USD por millón de tokens de
entrada; si cambia, se pasa con `--precio-mtok`. La cuenta de TypeSafe es la que vale: la
estimación sirve para comparar corridas.

## La clave de Jev

La aplicación lee `TYPESAFE_API_KEY` del entorno del sistema. **No** la lee de un archivo
`.env`: Spring Boot no tiene soporte para ese formato, y el `.env` del repositorio existe solo
para docker compose.

```bash
setx TYPESAFE_API_KEY "..."
```

Hay que abrir una terminal nueva después. Sin la clave, la evaluación corre igual y deja Jev
afuera del reporte.

## El dataset

Copiá `dataset.example.csv` a `dataset.csv` y completá la columna `expected_skill`.

**La etiqueta la pone una persona.** Es la competencia que de verdad correspondía, según el
criterio de alguien que atendió soporte. Si saliera de las mismas reglas que se quieren medir,
el motor acertaría siempre y el resultado no diría nada.

### El criterio

Etiquetar caso por caso, a ojo, no alcanza. Sin una regla escrita, dos tickets iguales
terminan en áreas distintas según el día, y un porcentaje calculado sobre eso no mide al
motor: mide la inconsistencia de quien etiquetó. Pasó en este dataset, con dos casos que eran
el mismo y quedaron al revés.

La regla es:

> **El ticket va al área dueña de la función donde aparece el problema, no al tipo de falla
> técnica.** `INFRASTRUCTURE` queda para lo que afecta a todo el sistema a la vez.

| Ticket | Va a | Y no a | Por qué |
|---|---|---|---|
| La descarga de factura queda colgada | `BILLING` | `INFRASTRUCTURE` | La función es facturación; que la falla sea técnica no cambia quién la conoce |
| Crear un usuario devuelve 500 | `ACCESS_IDENTITY` | `INFRASTRUCTURE` | Mismo caso que el anterior, y por eso la misma respuesta |
| Todas las pantallas devuelven 500 | `INFRASTRUCTURE` | — | Acá sí: no hay una función afectada, están todas |

Es como trabaja un helpdesk real. El ticket entra al equipo dueño de esa parte del producto,
y si al mirarlo resulta ser infraestructura, ese equipo lo escala. Etiquetar por el tipo de
error haría que infraestructura reciba casi todo, porque casi todo síntoma visible es técnico.

### Casos que el criterio no resuelve

Quedan tres, y están anotados en el encabezado de `dataset.csv` con el motivo. Vale dejarlos
adentro: un dataset donde todo es obvio hace quedar bien a cualquier estrategia y no sirve
para elegir entre ellas.

- **Dos problemas en un ticket.** El criterio no dice cuál gana. Queda el que bloquea el uso.
  En producción esto se parte en dos tickets, y el dataset no sabe representarlo.
- **Causa y síntoma en áreas distintas.** Un webhook que devuelve 403 porque cambiaron un
  permiso. Se etiqueta por donde aparece, que es lo que ve quien recibe el ticket.
- **Lo que no entra en ninguna de las cinco áreas.** Una app móvil que se cierra al abrir no
  es ninguna. Queda en `INFRASTRUCTURE` por descarte, y lo que eso señala no es una etiqueta
  mal puesta sino un área que falta en el catálogo.

Apuntá a 30 casos o más. Con menos, un acierto o un error mueven el porcentaje varios puntos.

## Decisiones de catálogo

Las descripciones de cada área las lee el modelo como criterio. Si una descripción contradice el
criterio de etiquetado, el motor y las etiquetas no coinciden, y el error se ve como fallo del
modelo cuando en realidad es una diferencia de definición.

- **Vigente: datos fiscales (CUIT, razón social, dirección fiscal) y cambios de medio de pago van
  a ACCOUNT_MANAGEMENT; los problemas con los pagos van a BILLING.** Es el catálogo de V2 y V4,
  restaurado por la migración V9 el 13 de noviembre de 2026.
- **Propuesta descartada (V8): esos temas pasaban a BILLING.** Se aplicó el 13 de noviembre y se
  evaluó con tres corridas por set, con V4 y con V8 (`--catalogo v8` la reproduce). En el set
  sintético, cuyas etiquetas son anteriores a V8, V8 tuvo 5 errores silenciosos más que V4: 92
  aciertos contra 96,7. En la validación con tickets nuevos la diferencia fue chica: 35,7 aciertos y
  8,3 errores silenciosos con V8, contra 34,3 y 7 con V4, con rangos superpuestos. Las etiquetas de
  ambos sets, incluida la de un etiquetador que no vio el catálogo, ponen estos temas en
  ACCOUNT_MANAGEMENT.
- **Cómo cambiar el catálogo después:** un cambio de descripciones se prueba con tickets nuevos,
  etiquetados antes de correr la evaluación con el cambio. No se ajusta mirando los sets que ya se
  usaron para decidir.

## Qué mide

| Métrica | Qué significa |
|---|---|
| Aciertos | Asignado a alguien con la competencia esperada |
| **Errores silenciosos** | Asignado a quien no correspondía, y nadie lo mandó a revisar |
| A revisión | Quedó esperando a una persona |
| Precisión al asignar | De lo que se animó a asignar, cuánto acertó |

Los **errores silenciosos** son la cifra que importa. Un ticket mal ruteado que cayó en la
bandeja lo arregla una persona; uno que pasó derecho no lo descubre nadie hasta que el cliente
vuelve a escribir.

Pero derivar no es gratis: cuesta tiempo de una persona. Una estrategia que manda todo a
revisión tiene cero errores silenciosos y tampoco resuelve nada, así que las dos columnas se
leen juntas.

## Qué se compara

1. **Reparto por carga**, sin leer el ticket. Es el piso: cualquier cosa que haga el motor
   tiene que ser mejor que esto.
2. **Palabras clave + reglas**, con el proveedor local. Mide cuánto alcanza con heurísticas.
3. **Jev + reglas**, cuando haya clave. Mide cuánto suma el modelo por encima de lo anterior.

La tercera se incluye cuando `TYPESAFE_API_KEY` está en el entorno; si no, se omite. Si al sumarla la mejora
sobre la segunda resulta chica, ese también es un resultado, y hay que publicarlo igual.

## Una limitación conocida de la línea base

El proveedor de palabras clave compara por subcadena, así que **es sensible a la forma de la palabra**:
"contraseña" en el ticket no coincide con "contraseñas" en el catálogo, ni "me cobraron" con "cobros". El caso termina en
revisión aunque sea claro para una persona.

Eso hace que la línea base sea débil, y una línea base débil infla la mejora de cualquier cosa
que venga después. Está anotado a propósito y no se ajustó el proveedor: cambiar una línea
base después de ver los resultados es la forma más fácil de fabricar una conclusión. Una
comparación más justa usaría raíces de palabras, y queda como mejora pendiente.

Al leer el reporte, conviene mirar cuántos casos derivó la línea base por
`SKILL_NOT_RECOGNIZED`: si son muchos, la diferencia con el modelo está exagerada.

## Supuestos

- El equipo contra el que se evalúa es sintético: un agente por competencia, todos
  disponibles, habilitados y con capacidad de sobra. Si la capacidad frenara asignaciones, el
  resultado mediría eso y no la elección de competencia.
- Todos los casos se evalúan con un SLA holgado, por el mismo motivo.
- Cada estrategia arranca con el equipo descargado, así la segunda no hereda el reparto de la
  primera.

## Lo que esta evaluación no mide

La calidad de la asignación dentro de la competencia correcta, el tiempo real de resolución y
la satisfacción del cliente. Mide una cosa: si el ticket llegó a alguien que podía resolverlo.

## Dataset de la primera corrida

Los números de esta sección salen de 100 tickets sintéticos. No son tickets de un cliente real.

- Los textos se escribieron en rol de diez usuarios distintos: una persona mayor, un comerciante, un estudiante, un contador, un cliente frustrado, un desarrollador, un adolescente, y usuarios de España y de México, entre otros. Hay 20 tickets por área.
- Nueve son casos difíciles, de tres tipos: dos problemas en un mensaje, causa y síntoma en áreas distintas, y algo que no entra en las cinco áreas.
- Los etiquetó a mano una persona que no vio la intención con la que se escribió cada texto. La intención se guardó aparte y no se usó como etiqueta.
- Una segunda persona etiquetó 15 de los 100, sin ver las etiquetas previas. Coincidieron en 15 de 15.
- El dataset no está en el repositorio.

## Resultados de la primera corrida

Corrida del 11 de noviembre de 2026, con `--dataset` apuntando a la hoja etiquetada.

| Estrategia | Aciertos | Errores silenciosos | A revisión | Precisión al asignar |
|---|---|---|---|---|
| Reparto por carga | 21 (21%) | 79 (79%) | 0 | 21% |
| Palabras clave + reglas | 1 (1%) | 0 | 99 (99%) | 100% (con una sola asignación) |
| Jev + reglas | 95 (95%) | 2 (2%) | 3 (3%) | 98% |

- Jev acierta 95 de 100. Con 100 casos, el intervalo de confianza del 95% va de 89% a 98%.
- Los dos errores silenciosos son distintos. Uno es un texto ambiguo, que no dice qué función falla. El otro es un caso de causa y síntoma: el modelo ruteó por la causa (un cambio de permisos) y el criterio lo manda a donde aparece el problema (la descarga de facturas).
- Causa y síntoma es lo más débil: de tres casos, ninguno se asignó bien. Los tres casos de dos problemas salieron bien.
- Sin los tres casos de frontera, Jev acierta 93 de 97. El resultado no depende de ellos.
- La línea base de palabras clave casi no decide: 99 tickets quedaron a revisión, 61 de ellos sin ninguna coincidencia. Por eso la diferencia de 94 puntos con Jev está exagerada, como advierte la sección sobre la línea base. La línea base no se cambió después de ver estos resultados.

## Límites de este resultado

- Los textos son sintéticos y más prototípicos que los reales. Con tickets reales, el acuerdo y la precisión van a ser más bajos.
- El acuerdo entre personas se midió en 15 casos. Con tan pocos, el intervalo es amplio: de 80% a 100%.
- Estos 100 son el set de prueba. Si se ajustan el prompt de Jev o las reglas mirando estos casos, el resultado deja de medir algo. Cualquier cambio se valida con tickets nuevos.
- Es una sola corrida. Con 100 casos, un ticket de más o de menos mueve el porcentaje en un punto.

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

Conviene que haya casos difíciles: los que mezclan dos áreas, los ambiguos, y los que no
encajan en ninguna competencia. Un dataset donde todo es obvio hace quedar bien a cualquier
estrategia y no sirve para elegir entre ellas.

Apuntá a 30 casos o más. Con menos, un acierto o un error mueven el porcentaje varios puntos.

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

La tercera todavía no está conectada: hace falta `TYPESAFE_API_KEY`. Si al sumarla la mejora
sobre la segunda resulta chica, ese también es un resultado, y hay que publicarlo igual.

## Una limitación conocida de la línea base

El proveedor de palabras clave compara por subcadena, así que **es sensible al plural**:
"contraseña" en el ticket no coincide con "contraseñas" en el catálogo, y el caso termina en
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

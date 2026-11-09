# ADR-009: Evaluación offline con dataset etiquetado a mano

- **Estado:** Aceptada
- **Fecha:** 2026-11-07

## Contexto

La tasa de override de la fase 5 solo ve los tickets que pasaron por revisión. Una asignación automática equivocada que nadie miró no aparece en ningún número: el cliente vuelve a escribir días después y el sistema nunca se entera.

Además falta responder la pregunta que justifica todo el proyecto: **¿el motor le gana a una heurística simple, y cuánto?** Sin esa comparación, "integramos un modelo" es una afirmación sin respaldo.

## Decisión

### Una simulación en memoria, no una corrida contra el sistema

La evaluación no crea tickets ni toca la base. Entra por el `main` con `--evaluate` y corta antes de levantar Spring. Es una herramienta de análisis, no una función del producto, y no debería dejar rastro en los datos reales ni exigir PostgreSQL para correr.

### Las etiquetas las pone una persona

El dataset se etiqueta a mano, con criterio de soporte. Si la etiqueta saliera de las mismas reglas que se quieren medir, el motor acertaría siempre y el resultado no diría nada. Por eso el repositorio trae `dataset.example.csv` con los textos y la columna de etiqueta **vacía**: completarla es trabajo de una persona, no del código.

El lector falla con el número de línea cuando encuentra filas sin etiquetar, en lugar de saltearlas. Un dataset a medias daría un porcentaje que parece válido sobre la mitad de los casos.

### La métrica principal son los errores silenciosos

Una asignación a quien no correspondía, que además no pasó por revisión. Es el error que nadie descubre.

Pero derivar tampoco es gratis: cuesta tiempo de una persona. Una estrategia que manda todo a revisión tiene cero errores silenciosos y no resuelve nada, así que el reporte muestra las dos columnas juntas y lo dice explícitamente. Hay un test que fija ese caso extremo.

### Se comparan tres estrategias

1. **Reparto por carga**, sin leer el ticket. Es el piso.
2. **Palabras clave + reglas**, con el proveedor local.
3. **Jev + reglas**, cuando haya clave.

El equipo contra el que se evalúa es sintético y deliberadamente holgado: todos disponibles, habilitados y con capacidad de sobra, y todos los casos con un SLA amplio. Si la capacidad o el vencimiento frenaran asignaciones, el resultado mediría eso en lugar de la elección de competencia.

Cada estrategia arranca con el equipo descargado, así la segunda no hereda el reparto de la primera.

## Consecuencias

**Positivas**
- Se puede responder con un número si el modelo aporta, y cuánto, sobre un conjunto fijo de casos.
- Corre sin base de datos y sin clave: el proveedor local alcanza para tener el reporte completo.
- El reporte sale en Markdown con el tamaño del dataset a la vista y la aclaración de que los datos son sintéticos. Un porcentaje sin denominador no se puede juzgar.
- El detalle por caso permite mirar en qué se equivocó y no solo cuánto.

**Negativas**
- **La línea base de palabras clave es débil, y eso infla la mejora de cualquier cosa que venga después.** Compara por subcadena, así que "contraseña" en el ticket no coincide con "contraseñas" en el catálogo y el caso termina en revisión. Una línea base más justa usaría raíces de palabras. Está anotado como limitación y no se "arregló" el proveedor para que luzca mejor ni peor: ajustar una línea base después de ver los resultados es la forma más fácil de fabricar una conclusión.
- El catálogo de competencias está duplicado entre la migración V2 y el CLI. Traerlo de la base obligaría a levantar el contexto para una herramienta que no lo necesita. Si cambia en V2, hay que cambiarlo en los dos lados.
- Mide una sola cosa: si el ticket llegó a alguien que podía resolverlo. No mide la calidad de la resolución, ni el tiempo real, ni la satisfacción del cliente.
- Con pocos casos los porcentajes se mueven mucho. El comando avisa por debajo de 30, pero no frena.

## Alternativas consideradas

- **Generar las etiquetas con el propio sistema:** instantáneo y sin trabajo manual, pero el resultado sería circular y no mediría nada.
- **Evaluar contra la base real creando tickets:** probaría también la persistencia, a costa de ensuciar los datos y de necesitar PostgreSQL para una herramienta de análisis.
- **Un plugin de Maven para el ejecutable:** se evitó usando un argumento del `main` que ya existe, sin sumar nada al build.
- **Medir solo la precisión:** una estrategia que deriva todo tendría precisión perfecta sobre lo poco que asigna. Por eso el reporte muestra aciertos sobre el total y errores silenciosos, no precisión sola.

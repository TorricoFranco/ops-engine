# ADR-015: Las semillas de desarrollo no bloquean las migraciones nuevas

- **Estado:** Aceptada
- **Fecha:** 2026-11-10

## Contexto

Las semillas de desarrollo son las migraciones `V900` y `V901` (`db/seed`). Comparten el historial de Flyway con el esquema, así que una migración de esquema agregada después tiene un número menor que las semillas: al agregar la traducción de las áreas, la `V6` quedó por debajo de la `V900`.

Flyway valida que toda migración resuelta esté aplicada. En una base que ya tiene la `V900`, la `V6` queda "fuera de orden" y la aplicación no arranca:

> Detected resolved migration not applied to database: 6.

## Decisión

En el perfil `dev`, `spring.flyway.out-of-order: true`. Flyway aplica la migración aunque su número sea menor que el de la última aplicada. Fuera de `dev` no hay semillas, así que no se activa y el orden sigue siendo el de siempre.

## Consecuencias

**Positivas**
- Una migración de esquema nueva llega a la base de desarrollo sin reconstruirla ni perder los datos de demo.

**Negativas**
- En `dev`, Flyway no protege del orden. Si una migración dependiera de datos de una semilla posterior, la falla aparecería en desarrollo y no antes. Hoy ninguna depende de eso.
- **Deuda:** la causa de fondo es que las semillas comparten el historial con el esquema. Separarlas (otro historial, o migraciones repetibles con inserts idempotentes) cambia cómo se cargan los datos de demo. Queda pendiente: no bloquea nada del MVP.

## Alternativas consideradas

- **Reconstruir la base de desarrollo:** descarta los datos de demo y es un comando destructivo.
- **Numerar las semillas por encima de las migraciones de esquema:** cada migración nueva tendría que superar a las semillas, y el historial quedaría con saltos.
- **Migraciones repetibles para las semillas:** es la práctica más limpia, pero cambia las semillas ya aplicadas y rompe la validación en la base actual.

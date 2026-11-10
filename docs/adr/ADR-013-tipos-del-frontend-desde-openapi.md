# ADR-013: Los tipos del frontend salen del OpenAPI, y el contrato publica enums

- **Estado:** Aceptada
- **Fecha:** 2026-11-10

## Contexto

El ADR-011 dejó anotada una deuda: el vocabulario del dominio estaba escrito dos veces. Los estados del ticket y los motivos de descarte y de derivación vivían como enums en el backend y como uniones de strings en `frontend/lib/api.ts`. Agregar un motivo nuevo en Java no rompía nada del lado del frontend; simplemente la pantalla pasaba a mostrar el enum crudo en inglés, que es justo lo que el ADR-011 se propuso evitar.

Al ir a resolverlo apareció que generar los tipos, por sí solo, no alcanzaba.

### El contrato publicaba texto libre donde había enums

`DecisionAuditController` y `ReviewController` convertían los enums a `String` con `Enum::name` antes de devolverlos. El JSON era correcto, pero el OpenAPI declaraba `type: string` sin valores posibles. Generar tipos desde ahí habría producido `string`: **tipos más débiles que los escritos a mano**, con la ceremonia de un generador y ninguna garantía.

### Dos esquemas distintos compartían nombre

`RoutingController` y `DecisionAuditController` tenían, cada uno, un record llamado `CandidateView`. springdoc indexa los esquemas por nombre simple, así que uno pisaba al otro: el OpenAPI venía describiendo uno de los dos endpoints con la forma del otro. Nadie lo había notado porque los dos records eran estructuralmente iguales, y dejaron de serlo en cuanto uno pasó a usar el enum.

### El endpoint de métricas publicaba un tipo interno

`GET /api/review/metrics` devolvía `ReviewMetrics.Snapshot`, un record de la capa de aplicación. El contrato traía un esquema llamado `Snapshot`, y renombrar ese record interno habría cambiado la API sin que nadie se enterara.

## Decisión

### El backend publica los enums como enums

Los DTOs devuelven el tipo enum en lugar de `String`. Jackson lo serializa con el mismo texto, así que **el JSON no cambia** y ningún cliente se entera; lo que cambia es que el OpenAPI ahora declara los valores posibles.

Los dos `CandidateView` llevan `@Schema(name = ...)` explícito, y las métricas tienen su propio `MetricsView` en el controlador, como el resto de los endpoints.

### El frontend genera sus tipos y no los escribe

`openapi-typescript` convierte el contrato en `lib/api.generated.ts`. `lib/api.ts` solo le pone nombres cortos a esos esquemas y deriva las uniones de valores de los campos donde aparecen.

### Los diccionarios de traducción son exhaustivos

Es la parte que hace que esto sirva de algo. `REJECTION_LABEL` y `REVIEW_LABEL` pasaron de `Record<string, string>` a `Record<RejectionReason, string>` y `Record<ReviewReason, string>`. Si el backend agrega un valor y nadie traduce, **el build falla con el nombre de la clave que falta**.

Se verificó agregando un motivo de prueba al enum: `tsc` falló señalando `ON_VACATION`, y el test de contrato también. Después se revirtió.

### El `openapi.json` se versiona

El contrato se commitea. Permite regenerar sin la aplicación corriendo, y hace que un cambio de API aparezca como diff en el pull request en vez de ser invisible.

Encima de eso hay un test que lee ese archivo y compara los valores de cada enum contra las claves de los diccionarios. `tsc` ya cubre el caso de "agregaron un valor y no lo traduje", pero solo si alguien regeneró los tipos; el test cubre además "cambió el contrato y `api.generated.ts` quedó viejo".

### Todo campo es opcional, salvo los que se declaran ausentes

springdoc no marca nada como `required`, así que el esquema generado deja todo opcional. En vez de propagar un `undefined` que no existe, un tipo `Present<T, Ausentes>` marca presente todo menos los campos que el backend puede dejar en nulo, que se enumeran a mano con su motivo.

Esa lista corta es conocimiento del dominio —cuándo no hay juicio, cuándo no hay asignado, cuándo una tasa no tiene divisor— y no se puede deducir del esquema actual.

## Consecuencias

**Positivas**
- Agregar un estado o un motivo en el backend rompe el build del frontend hasta que alguien escriba el texto en castellano. Era el objetivo.
- Los tipos describen el JSON real. Los fixtures de los tests usaban `null` donde la API omite el campo, porque está configurada con `default-property-inclusion: non_null`; ahora omiten, como el backend. Esa discrepancia ya había causado un bug visible: una tasa que llegaba `undefined` y se mostraba como `NaN%`.
- El OpenAPI dejó de mentir sobre uno de los dos endpoints de candidatos.
- Desaparecieron los fallbacks del tipo `LABEL[x] ?? x`, que existían para un caso que el tipo ahora vuelve imposible.

**Negativas**
- **La regeneración es manual.** `npm run types:api:fetch` necesita la aplicación corriendo. Hacerlo en CI exigiría levantar la API con su base de datos solo para producir un JSON. Mientras tanto, lo que protege es el test de contrato: si alguien cambia la API y no regenera, falla.
- **La lista de campos ausentes se mantiene a mano.** Si un campo se vuelve nulo en el backend, el frontend no se entera. Se arreglaría anotando los DTOs con `@NotNull` para que springdoc emita `required`, que son unas cuarenta anotaciones y queda pendiente.
- Hay un archivo generado de 762 líneas en el repositorio. Es ruido en los diffs, a cambio de que el cambio de contrato sea visible.
- El frontend depende ahora de la forma del contrato, no solo de su contenido. Renombrar un DTO en Java rompe el frontend, que es exactamente lo que se buscaba, pero conviene saberlo antes de renombrar.

## Alternativas consideradas

- **Seguir escribiendo los tipos a mano:** cero herramientas, y la deuda del ADR-011 intacta.
- **Generar también el cliente HTTP** con `openapi-generator`: trae un cliente entero con sus abstracciones a cambio de reemplazar un `fetch` de treinta líneas que ya funciona y que maneja el token y los 401 como este proyecto los necesita.
- **Dejar los `String` y escribir las uniones a mano igual:** era el estado anterior disfrazado de generación.
- **Validar en runtime con Zod:** verifica que la respuesta cumpla el esquema, que es otro problema. No ayuda a que falte una traducción, y agrega una dependencia y un esquema más que mantener.

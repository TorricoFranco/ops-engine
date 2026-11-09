# ADR-011: Una consola que explica el motor sin leer código

- **Estado:** Aceptada
- **Fecha:** 2026-11-09

## Contexto

El backend ya resolvía todo: juzgaba, decidía, auditaba y medía. Pero para entender qué hacía había que leer código o pegarle a la API con `curl`. Eso filtra a casi todo el mundo, incluido quien evalúa un portfolio en dos minutos.

La pregunta no era "¿hace falta un frontend?" sino "¿qué tiene que mostrar para que el sistema se entienda solo?".

## Decisión

### Una sola pantalla carga con el peso

El detalle de la decisión está partido en dos columnas: **lo que estimó el modelo** a la izquierda, **lo que decidió el código** a la derecha. Esa separación es la tesis del proyecto, así que la interfaz la hace literal en lugar de mezclar las dos cosas en una lista.

La columna del modelo aclara, al pie, que son estimaciones y que el modelo no sabe quién está disponible. Sin esa línea, la pantalla se lee como que decidió la IA, que es exactamente lo contrario de lo que pasa. Hay un test que la verifica.

La columna del código muestra cada candidato con su motivo de descarte **en castellano**: "sin habilitación para críticos", no `INSUFFICIENT_CLEARANCE`. Mostrar el enum obligaría a conocer el código para leer la pantalla. También hay un test.

### Next.js, Tailwind, sin biblioteca de componentes

`CLAUDE.md` preveía shadcn/ui. No se usó: los componentes que hacen falta son tarjetas, badges, barras y tablas, todos triviales en Tailwind, y el instalador interactivo agregaba una dependencia y un paso frágil a cambio de nada visible. Quien mira una captura ve píxeles, no el `package.json`.

Si más adelante hace falta algo con comportamiento real, como un diálogo accesible o un combobox, conviene reconsiderarlo.

### Tema oscuro

No es gusto: las barras de probabilidad y los estados se leen de un vistazo en una imagen chica. Una parte del público llega desde una captura en una red social, no desde el repositorio.

### Datos de demo que cubren el abanico

Un cargador del perfil `dev` crea diez tickets y los rutea. Están elegidos para que se vean los tres finales posibles: asignaciones claras, uno ilegible que termina pidiendo datos y uno fuera de alcance que va a revisión. También hay más de un agente por área, con alguien sin habilitación, alguien lleno y alguien de licencia, para que la lista de descartes tenga algo que mostrar.

Sin eso, la primera impresión del panel es una pantalla vacía.

### CORS con orígenes explícitos

Nada de comodín. La API responde con credenciales, y un comodín dejaría que cualquier sitio las use desde el navegador de quien esté logueado.

## Consecuencias

**Positivas**
- El sistema se entiende mirándolo. Una captura del detalle de la decisión cuenta la historia completa.
- El frontend consume la misma API pública que cualquier otro cliente: no hay endpoints hechos a medida de la pantalla.
- Los textos de la interfaz traducen los enums del dominio, así que el vocabulario técnico no se filtra a quien usa el sistema.

**Negativas**
- **El vocabulario está duplicado.** Los nombres de estados y motivos viven en el backend como enums y en el frontend como diccionarios de traducción. Un motivo nuevo exige tocar los dos lados, y si se olvida, la pantalla muestra el enum crudo. Generar los tipos desde el OpenAPI resolvería la mitad del problema y queda pendiente.
- **No hay tests de punta a punta.** Playwright estaba en el plan y no se hizo: los tests cubren componentes, no el flujo completo contra la API.
- El panel recarga pidiendo todo de nuevo, sin caché ni actualización incremental. Con esta cantidad de datos no importa.
- Es una segunda aplicación en el mismo repositorio, con su propio build y su propio job de CI.

## Alternativas consideradas

- **No hacer frontend y mostrar Swagger:** gratis, pero exige leer JSON para entender una decisión, y no comunica nada a quien no programa.
- **Renderizar en el servidor con Thymeleaf:** una aplicación menos, pero el plan ya definía Next.js y el objetivo incluía demostrar frontend.
- **shadcn/ui:** buena señal en un portfolio, pero el valor visible era el mismo y sumaba dependencia e instalador.
- **Tema claro:** más convencional, menos legible en una captura chica, que es donde mucha gente lo va a ver por primera vez.

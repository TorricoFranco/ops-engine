# ADR-014: Cada área tiene un nombre en la base, y la regla no la nombra por código

- **Estado:** Aceptada
- **Fecha:** 2026-11-10

## Contexto

Las áreas de soporte son filas de la tabla `skills`, y agregar una es una migración, no código (`CLAUDE.md`). La consola, sin embargo, mostraba el código en inglés (`BILLING`, `INFRASTRUCTURE`) en el detalle de la decisión, en la bandeja de revisión y en el formulario de asignación. Eso contradice lo que dice el ADR-011: el vocabulario técnico no se filtra a quien usa el sistema.

El problema tenía una segunda capa. Los textos que genera la regla (`RoutingRules`) incluían el código: "Carla tiene la competencia BILLING y es quien menos carga tiene". Como el texto lo arma el backend, el frontend no lo podía traducir.

## Decisión

1. **Cada área tiene un nombre para la persona**, en castellano, además de la descripción que lee el modelo. Son dos cosas distintas: la descripción es larga y está pensada para el modelo; el nombre es corto y es lo que lee quien usa la consola. La migración V6 los carga para las cinco áreas.

2. **El catálogo se expone por la API.** `GET /api/skills` devuelve código y nombre, y la consola lo usa para mostrar cada área. Ninguna lista de áreas vive en el frontend. Si un área no trae nombre, la pantalla muestra el código: se nota, y se corrige con una fila. La columna es obligatoria, así que no debería pasar.

3. **Los textos de la regla no nombran la competencia por código.** Dicen "la competencia del ticket". El código sigue en los datos (candidatos, auditoría, API). La persona lee el nombre que la consola toma del catálogo.

## Consecuencias

**Positivas**
- Agregar un área es una fila con código, descripción y nombre. El backend y el frontend no cambian.
- La consola queda en castellano sin un diccionario que alguien tenga que mantener.
- Un test de integración fija los nombres de las cinco áreas, y los tests de reglas fijan que el texto no lleva código.

**Negativas**
- **Las decisiones anteriores conservan el texto viejo.** La auditoría es append-only y no se reescribe: los registros de antes de la migración todavía dicen "BILLING" dentro de la razón. La consola traduce el área en cualquier decisión, pero el texto de la regla queda como se escribió entonces.
- **El nombre vive en la base.** Cambiar la redacción de un nombre es una migración, no un cambio de código. Es lo que se buscaba, pero conviene saberlo.
- Un área sin nombre se ve como código. Es una elección: preferimos que se note antes que inventar un texto.

## Alternativas consideradas

- **Diccionario en el frontend** (`BILLING → Facturación`): rápido, pero cada área nueva obliga a tocar el frontend, y no alcanza para los textos que genera el backend.
- **Nombre dentro del dominio** (`Skill` con código y nombre): el dominio cargaría textos de presentación en cada objeto, en todo el código, para mostrar algo que solo usa la consola.
- **Incluir el nombre en cada respuesta** (candidatos, juicios, empleados): repite el dato en cada fila. Un endpoint de catálogo alcanza para la consola.

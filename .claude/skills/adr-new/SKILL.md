---
name: adr-new
description: Crea un ADR nuevo y numerado en docs/adr/ con la plantilla del proyecto. Usar cuando haya que registrar una decisión de arquitectura, de stack o de una dependencia nueva.
---

# Crear un ADR

1. Listar `docs/adr/` y tomar el número más alto. El nuevo es el siguiente, con formato `ADR-NNN` de tres dígitos.
2. Nombre de archivo: `ADR-NNN-titulo-en-kebab-case.md`.
3. Usar esta estructura, igual que `docs/adr/ADR-001-spring-boot-java21.md`:
   - Título: `# ADR-NNN: <título corto>`
   - `**Estado:** Propuesta | Aceptada | Reemplazada por ADR-XXX`
   - `**Fecha:** AAAA-MM-DD` (fecha de hoy)
   - `## Contexto`: el problema y las restricciones, sin proponer la solución todavía.
   - `## Decisión`: qué se decidió, en viñetas concretas.
   - `## Consecuencias`: subtítulos **Positivas** y **Negativas**. Las negativas son obligatorias.
   - `## Alternativas consideradas`: cada alternativa con el motivo por el que se descartó.
4. Redactar en español. Los identificadores de código van en inglés.
5. Si la decisión cambia algo de `docs/plan.md`, actualizar ese archivo en el mismo cambio. La lista de la sección 9 también.
6. Nunca editar el contenido de un ADR aceptado. Si cambia la decisión, crear un ADR nuevo que reemplace al anterior y cambiar solo la línea de estado del viejo a `Reemplazada por ADR-XXX`.
7. No commitear sin que el usuario lo pida.

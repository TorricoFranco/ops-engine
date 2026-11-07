# ADR-008: Auditoría de decisiones y registro del desacuerdo humano

- **Estado:** Aceptada
- **Fecha:** 2026-11-07

## Contexto

Hasta la fase 4 el resultado del ruteo se devolvía en la respuesta HTTP y se perdía. Eso alcanza para mirar una decisión en el momento, pero no para lo que el proyecto quiere mostrar: que las decisiones se pueden reconstruir meses después, y que se puede medir en qué se equivoca el motor.

Sin registro no hay forma de responder dos preguntas: por qué este ticket terminó donde terminó, y con qué frecuencia una persona contradice al sistema.

## Decisión

### Qué se guarda

Una fila en `decision_records` por cada decisión de ruteo, se haya asignado o haya ido a revisión, con:

- el proveedor que respondió y la versión de reglas que se aplicó;
- las probabilidades del juicio, no solo la conclusión;
- el resultado, a quién se asignó o por qué fue a revisión, y el texto que lo explica;
- cada candidato evaluado con su motivo de descarte, en `decision_candidates`;
- la latencia de la decisión.

Las probabilidades se guardan enteras porque sin ellas no se puede saber si una decisión fue ajustada o clarísima, que es justo lo que hace falta para mover un umbral con criterio.

Los candidatos van en tabla aparte y no como JSON, para poder contar después con qué frecuencia descarta cada motivo.

### Qué no se guarda

**El texto del ticket.** Está en su propia tabla y copiarlo multiplicaría los datos personales sin aportar nada para reconstruir la decisión (RNF-04). La auditoría referencia el ticket por id.

### El registro es append-only

Una decisión no se corrige: se agrega otra. Lo que hace la persona después vive en `review_resolutions`, separado, así nunca se confunde qué decidió el sistema con qué decidió alguien.

### El desacuerdo exige motivo

Al resolver una revisión, el supervisor dice si coincidió con lo que sugirió el sistema. Cuando no coincide, el motivo es obligatorio, y lo exige tanto el dominio como una restricción en la base. Un desacuerdo sin motivo deja un número que dice que el motor se equivocó pero no en qué, y con eso no se corrige nada.

**Sin sugerencia no hay acuerdo ni desacuerdo.** Cuando el proveedor no respondió, el campo queda nulo en lugar de contarse como override: mezclar una falla de infraestructura con un error de criterio del modelo ensuciaría la métrica.

Por el mismo motivo, la tasa de override viene vacía mientras no haya casos comparables. Un cero inventado se leería como que el motor nunca se equivoca.

## Consecuencias

**Positivas**
- Cualquier decisión se puede reconstruir: entrada resumida, versión de reglas, salida del proveedor y acción (RF-09).
- La tasa de override y la de revisión salen de datos guardados, así que sobreviven a un reinicio y se pueden recalcular.
- Los motivos de descarte quedan contables: se puede ver si lo que frena las asignaciones es la capacidad, la disponibilidad o la habilitación.

**Negativas**
- `decision_records` crece con cada ruteo y todavía no hay archivado ni purga. Hay que agregarlos antes de que el volumen importe.
- La auditoría se escribe en la misma transacción que el cambio, así que una falla al registrar aborta el ruteo. Es deliberado, porque una decisión aplicada que no se puede explicar es peor que una que no se aplicó, pero significa que la auditoría es parte del camino crítico.
- **`MISSING_SKILL` no aparece en la auditoría.** El caso de uso pide los candidatos ya filtrados por competencia, así que nunca se evalúa a alguien que no la tenga. La regla existe igual, porque las reglas no deben asumir que quien las llama filtró antes, pero en el flujo actual es una rama defensiva. Mostrar a todos los empleados exigiría traerlos a todos en cada ruteo.
- La tasa de override solo cubre los tickets que pasaron por revisión. Una asignación automática equivocada que nadie miró no se cuenta: eso son los errores silenciosos, y la evaluación offline de la fase 6 es la que los mide.

## Alternativas consideradas

- **Guardar los candidatos como JSON:** menos tablas, pero deja de poder contarse por motivo sin parsear.
- **Registrar la decisión fuera de la transacción:** sacaría la auditoría del camino crítico, a costa de poder quedarse sin el registro de un cambio que sí ocurrió.
- **Deducir el override comparando al empleado elegido con la sugerencia:** evita preguntarle al supervisor, pero solo detecta desacuerdos sobre la persona, no sobre el criterio, y nunca captura el motivo.
- **Usar el outbox como auditoría:** el outbox es para entregar, se purga y es de entrega *al menos una vez*. Mezclarlo con el registro permanente confundiría dos cosas distintas.

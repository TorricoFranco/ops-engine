# ADR-007: Optimistic locking con la versión en el aggregate

- **Estado:** Aceptada
- **Fecha:** 2026-11-07

## Contexto

Asignar un ticket cambia dos cosas: el estado del ticket y la carga del empleado. Si dos asignaciones ocurren a la vez sobre el mismo empleado, las dos leen la misma carga, las dos le suman uno y la última escritura pisa a la primera. El resultado es un empleado por encima de su capacidad sin que nadie se entere, que es exactamente el límite que el ruteo dice respetar.

En la fase 2 las tablas ya tenían columna `version`, pero los adapters construían una entidad nueva en cada guardado, siempre con versión cero. Eso no protegía nada: la versión no viajaba desde la lectura hasta la escritura.

## Decisión

- La versión forma parte del aggregate: `Ticket` y `Employee` la reciben al reconstruirse desde la base y la devuelven al guardarse.
- El adapter la pasa a la entidad JPA, y Hibernate emite `update ... where version = ?`. Si otra transacción escribió en el medio, el update no afecta ninguna fila y falla con `OptimisticLockingFailureException`.
- Antes de asignar, el caso de uso vuelve a pedirle permiso al aggregate con `takeTicket`, que revalida competencia, disponibilidad y capacidad. Entre que se leyeron los candidatos y se decide, la carga pudo cambiar.
- Se eligió optimista y no pesimista: los conflictos son raros, y bloquear filas de empleados en cada ruteo sería un cuello de botella.

## Consecuencias

**Positivas**
- Dos asignaciones simultáneas no se pisan, y hay un test que lo demuestra con dos hilos reales contra PostgreSQL.
- No hay bloqueos: la lectura de candidatos no traba a nadie.
- La versión viajando por el dominio deja explícito que el aggregate se leyó en un momento concreto.

**Negativas**
- El conflicto llega hasta el que llamó. Hoy se traduce a 409 y el pedido se repite desde afuera; no hay reintento automático.
- El dominio carga con un dato que es de persistencia. Es una concesión consciente: la alternativa era dejar entidades JPA manejadas circulando por los casos de uso.
- La versión primitiva (`long`) impide que Spring Data detecte una entidad nueva por ese campo, así que el guardado siempre hace un `merge` y agrega un SELECT antes del INSERT.

## Alternativas consideradas

- **Bloqueo pesimista (`SELECT ... FOR UPDATE`):** evita el conflicto en lugar de detectarlo, a costa de serializar las asignaciones sobre el mismo empleado.
- **Contador atómico en SQL (`UPDATE ... SET open_tickets = open_tickets + 1 WHERE open_tickets < max`):** correcto y más barato, pero mueve la regla de capacidad del dominio a una sentencia SQL, donde deja de poder probarse con un test unitario.
- **Dejar que el reintento lo haga el servidor:** se descartó por ahora. Reintentar una asignación puede terminar eligiendo otro empleado, y esa decisión debería quedar registrada, no esconderse.

# ADR-016: Tope de reintentos del ruteo

- **Estado:** Aceptada
- **Fecha:** 2026-11-11

## Contexto

El ruteo corre en un worker de fondo (ADR-010). Si la escritura del resultado o el propio ruteo fallan con un error inesperado, el ticket queda en `NUEVO` y la pasada siguiente lo vuelve a tomar. Nada cuenta cuántas veces ya falló.

Eso deja un hueco: un ticket que falla siempre, por un dato que el ruteo no maneja o por un error que se repite, vuelve en cada pasada para siempre. Ocupa capacidad del worker, llena el log con el mismo error y no llega a ninguna persona, porque la cola de revisión no lo muestra. ADR-010 ya lo había anotado como pendiente.

Restricciones: el juicio del proveedor sigue fuera de la transacción (ADR-010); ningún ticket puede perderse, porque es la solicitud de un cliente; y el conteo tiene que sobrevivir a un reinicio y funcionar con más de una instancia del worker.

## Decisión

- Un fallo inesperado del ruteo deja el ticket en `NUEVO` y cuenta un intento en el propio ticket (columna `routing_attempts`, migración V7). Una falla del proveedor no entra acá: sigue yendo a revisión con su motivo, como antes.
- El tope es `app.routing.worker.max-attempts`, con 5 como valor por defecto.
- Al llegar al tope, el ticket pasa a revisión humana (`REVISION_REQUERIDA`) con el motivo `ROUTING_FAILED`. Se registra la decisión sin juicio y se publica el evento `TICKET_SENT_TO_REVIEW` en el outbox.
- El registro guarda solo el nombre de la excepción. Su mensaje puede contener datos del ticket, así que no se guarda ni se loguea.
- Si registrar el intento también falla (por ejemplo, porque la base no responde), se loguea y el ticket queda en `NUEVO` para la próxima pasada. Un error de registro no corta la pasada.

## Consecuencias

**Positivas**
- Un ticket que siempre falla deja de ocupar el worker y llega a una persona, con el nombre del error como motivo.
- El conteo vive en la fila del ticket: sobrevive a reinicios y funciona con varias instancias.
- Las revisiones por este motivo entran en `ops_routing_decisions_total` con `reason="ROUTING_FAILED"`, así que aparecen en el tablero.

**Negativas**
- Un fallo sistémico, como un bug que afecta a todos los tickets, manda a revisión a todos al llegar al tope. Es correcto, porque una persona lo ve, pero la cola puede llenarse de golpe.
- Cinco fallos seguidos del mismo ticket bastan para mandarlo a una persona, aunque el último error haya sido transitorio. El umbral de 5 es una propuesta: no hay tráfico real para calibrarlo.
- Quien revisa solo ve el nombre de la excepción. El detalle queda en el log, que se consulta con el id del ticket.
- El registro va en la misma transacción que el cambio, así que una caída de la base no cuenta intentos. Es intencional, pero un ticket puede reintentarse varias veces durante una caída larga.
- Varias instancias pueden seguir tomando el mismo ticket a la vez: eso lo resuelve `SKIP LOCKED`, que sigue pendiente (ADR-010). Este cambio no lo toca.

## Alternativas consideradas

- **Contador en memoria en el worker:** se pierde al reiniciar y cada instancia contaría por separado. Un ticket podría seguir reintentándose para siempre entre reinicios.
- **Descartar el ticket al llegar al tope:** deja de reintentarse, pero la solicitud de un cliente desaparece sin que nadie la vea.
- **Estado terminal nuevo, por ejemplo `DESCARTADO`:** cambia el catálogo de estados, las transiciones y la consola, cuando la revisión humana ya existe y alcanza.
- **Reintentos con espera exponencial sin tope:** espacia los intentos, pero el ticket sigue sin llegar a nadie.
- **Dejar el comportamiento como estaba y confiar en el log y en la cantidad de pendientes:** el ticket sigue invisible para las personas que atienden la cola.

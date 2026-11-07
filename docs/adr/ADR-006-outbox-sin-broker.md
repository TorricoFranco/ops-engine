# ADR-006: Outbox transaccional en lugar de un broker

- **Estado:** Aceptada
- **Fecha:** 2026-11-07

## Contexto

Cuando un ticket se asigna o pasa a revisión hay que avisarle al resto del sistema: notificaciones, métricas y, más adelante, la bandeja de revisión del frontend.

Publicar directo a un broker desde el caso de uso tiene un problema conocido: la publicación y el cambio en la base no comparten transacción. Si el mensaje sale y después la transacción falla, queda un evento de una asignación que nunca ocurrió. Si la transacción confirma y la publicación falla, la asignación queda sin aviso.

## Decisión

- El evento se escribe en la tabla `outbox_events` dentro de la misma transacción que el cambio que lo produce. O se guardan los dos, o no se guarda ninguno.
- Un `@Scheduled` lee los pendientes y los publica. Un fallo deja el evento sin marcar y se reintenta en la pasada siguiente, contando intentos y guardando el último error.
- **Hoy el destino es el log.** No hay broker: meter RabbitMQ solo para esto queda fuera del MVP. Lo que importa ya está resuelto, que es la garantía transaccional; cuando haya un broker se reemplaza el cuerpo de `publish`.
- Un índice parcial sobre los no publicados mantiene la consulta del despachador barata aunque la tabla crezca.
- El despachador se puede apagar con `app.outbox.dispatcher.enabled=false`, que es lo que hacen los tests para poder inspeccionar los eventos pendientes sin carreras.

## Consecuencias

**Positivas**
- No puede haber una asignación sin su evento, ni un evento de algo que no pasó.
- Ningún evento se pierde por una caída del destino: queda pendiente y se reintenta.
- La cantidad de pendientes y la antigüedad del más viejo son métricas de salud directas (ver `docs/monitoring.md`).

**Negativas**
- La entrega es *al menos una vez*: un evento publicado cuya marca no llegó a guardarse se vuelve a publicar. Los consumidores tienen que ser idempotentes.
- Hay latencia: el evento sale hasta cinco segundos después del cambio.
- La tabla crece y todavía no hay purga de publicados. Hay que agregarla antes de que el volumen importe.
- Un solo despachador: con varias instancias haría falta `SELECT ... FOR UPDATE SKIP LOCKED` para que no se pisen.

## Alternativas consideradas

- **Publicar directo al broker:** menos piezas, pero abre la ventana entre la transacción y la publicación, que es justo lo que se quiere evitar.
- **Eventos de Spring (`ApplicationEventPublisher`) con `@TransactionalEventListener`:** resuelve el orden respecto de la transacción, pero el evento vive en memoria: si el proceso se cae entre el commit y la entrega, se pierde.
- **Change Data Capture con Debezium:** no toca el código de la aplicación, pero suma Kafka y Connect a un proyecto que todavía no los necesita.

package com.franco.opsengine.outbox.adapter.scheduler;

import com.franco.opsengine.outbox.application.OutboxRepository;
import com.franco.opsengine.outbox.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Saca los eventos pendientes del outbox.
 *
 * <p>Hoy el destino es el log: todavía no hay un broker, y meter RabbitMQ solo para esto
 * quedaría fuera del MVP. Lo que importa ya está resuelto, que es la garantía: el evento se
 * guardó en la misma transacción que el cambio, así que publicarlo es un problema aparte y
 * reintentable. Cuando haya un broker, se reemplaza el cuerpo de {@link #publish}.
 *
 * <p>Se puede apagar con app.outbox.dispatcher.enabled=false, que es lo que hacen los tests
 * que quieren inspeccionar los eventos sin que el despachador se los lleve antes.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.dispatcher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outbox;

    public OutboxDispatcher(OutboxRepository outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatcher.interval-ms:5000}")
    public void dispatchPending() {
        for (OutboxEvent event : outbox.findPending(BATCH_SIZE)) {
            try {
                publish(event);
                outbox.markPublished(event.id());
            } catch (RuntimeException e) {
                // Queda pendiente y se reintenta en la próxima pasada. No se pierde.
                log.warn("Outbox event could not be published eventId={} type={} reason={}",
                        event.id(), event.type(), e.getMessage());
                outbox.markFailed(event.id(), e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void publish(OutboxEvent event) {
        log.info("Outbox event published eventId={} type={} aggregateId={}",
                event.id(), event.type(), event.aggregateId());
    }
}

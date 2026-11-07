package com.franco.opsengine.outbox.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un evento pendiente de publicar.
 *
 * <p>Se escribe en la misma transacción que el cambio que lo produce. Así no puede quedar una
 * asignación sin su evento ni un evento de una asignación que al final no ocurrió, que es lo
 * que pasaría si se publicara directo a un broker antes de confirmar la transacción.
 */
public record OutboxEvent(
        UUID id,
        String type,
        UUID aggregateId,
        String payload,
        Instant occurredAt) {

    public OutboxEvent {
        Objects.requireNonNull(id, "event id is required");
        Objects.requireNonNull(aggregateId, "aggregate id is required");
        Objects.requireNonNull(occurredAt, "the time of the event is required");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("the event needs a type");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("the event needs a payload");
        }
    }

    public static OutboxEvent of(String type, UUID aggregateId, String payload, Instant occurredAt) {
        return new OutboxEvent(UUID.randomUUID(), type, aggregateId, payload, occurredAt);
    }
}

package com.franco.opsengine.outbox.application;

import com.franco.opsengine.outbox.domain.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    /** Se llama dentro de la transacción del cambio que produjo el evento. */
    void record(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    void markPublished(UUID eventId);

    void markFailed(UUID eventId, String error);

    long countPending();
}

package com.franco.opsengine.outbox.adapter.persistence;

import com.franco.opsengine.outbox.application.OutboxRepository;
import com.franco.opsengine.outbox.domain.OutboxEvent;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Repository
class OutboxRepositoryAdapter implements OutboxRepository {

    private final OutboxJpaRepository jpa;
    private final Clock clock;

    OutboxRepositoryAdapter(OutboxJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void record(OutboxEvent event) {
        jpa.save(new OutboxEventEntity(
                event.id(), event.type(), event.aggregateId(), event.payload(), event.occurredAt()));
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jpa.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(limit)).stream()
                .map(entity -> new OutboxEvent(entity.getId(), entity.getType(), entity.getAggregateId(),
                        entity.getPayload(), entity.getOccurredAt()))
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        jpa.findById(eventId).ifPresent(entity -> {
            entity.markPublished(clock.instant());
            jpa.save(entity);
        });
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        jpa.findById(eventId).ifPresent(entity -> {
            entity.markFailed(error);
            jpa.save(entity);
        });
    }

    @Override
    public long countPending() {
        return jpa.countByPublishedAtIsNull();
    }
}

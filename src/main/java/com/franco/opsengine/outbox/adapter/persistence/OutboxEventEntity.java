package com.franco.opsengine.outbox.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected OutboxEventEntity() {
        // requerido por JPA
    }

    OutboxEventEntity(UUID id, String type, UUID aggregateId, String payload, Instant occurredAt) {
        this.id = id;
        this.type = type;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.attempts = 0;
    }

    void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
    }

    UUID getId() {
        return id;
    }

    String getType() {
        return type;
    }

    UUID getAggregateId() {
        return aggregateId;
    }

    String getPayload() {
        return payload;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    Instant getPublishedAt() {
        return publishedAt;
    }

    int getAttempts() {
        return attempts;
    }
}

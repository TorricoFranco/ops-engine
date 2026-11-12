package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.ticket.domain.Impact;
import com.franco.opsengine.ticket.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Tabla `tickets`. Es un contenedor de datos: las reglas viven en el aggregate del dominio.
 */
@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Impact impact;

    @Column(name = "required_skill", nullable = false, length = 50)
    private String requiredSkill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "review_reason", columnDefinition = "text")
    private String reviewReason;

    @Column(name = "routing_attempts", nullable = false)
    private int routingAttempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sla_deadline", nullable = false)
    private Instant slaDeadline;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private long version;

    protected TicketEntity() {
        // requerido por JPA
    }

    TicketEntity(UUID id, String title, String description, Impact impact, String requiredSkill,
                 TicketStatus status, UUID assigneeId, String reviewReason, Instant createdAt,
                 Instant slaDeadline, String idempotencyKey, int routingAttempts, long version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.impact = impact;
        this.requiredSkill = requiredSkill;
        this.status = status;
        this.assigneeId = assigneeId;
        this.reviewReason = reviewReason;
        this.createdAt = createdAt;
        this.slaDeadline = slaDeadline;
        this.idempotencyKey = idempotencyKey;
        this.routingAttempts = routingAttempts;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getDescription() {
        return description;
    }

    Impact getImpact() {
        return impact;
    }

    String getRequiredSkill() {
        return requiredSkill;
    }

    TicketStatus getStatus() {
        return status;
    }

    UUID getAssigneeId() {
        return assigneeId;
    }

    String getReviewReason() {
        return reviewReason;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getSlaDeadline() {
        return slaDeadline;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    int getRoutingAttempts() {
        return routingAttempts;
    }

    long getVersion() {
        return version;
    }
}

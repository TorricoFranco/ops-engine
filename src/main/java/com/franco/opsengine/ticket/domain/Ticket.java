package com.franco.opsengine.ticket.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root del ticket. El estado solo cambia a través de sus métodos,
 * que validan la transición antes de aplicarla.
 */
public class Ticket {

    private static final int MAX_TITLE_LENGTH = 200;

    private final TicketId id;
    private final String title;
    private final String description;
    private final Impact impact;
    private final Skill requiredSkill;
    private final Instant createdAt;
    private final Instant slaDeadline;
    private final long version;
    private TicketStatus status;
    private EmployeeId assignee;
    private String reviewReason;

    private Ticket(TicketId id, String title, String description, Impact impact, Skill requiredSkill,
                   Instant createdAt, Instant slaDeadline, long version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.impact = impact;
        this.requiredSkill = requiredSkill;
        this.createdAt = createdAt;
        this.slaDeadline = slaDeadline;
        this.version = version;
        this.status = TicketStatus.NUEVO;
    }

    public static Ticket open(TicketId id, String title, String description, Impact impact, Skill requiredSkill,
                              Instant createdAt, Instant slaDeadline) {
        Objects.requireNonNull(id, "ticket id is required");
        Objects.requireNonNull(impact, "impact is required");
        Objects.requireNonNull(requiredSkill, "required skill is required");
        Objects.requireNonNull(createdAt, "creation time is required");
        Objects.requireNonNull(slaDeadline, "SLA deadline is required");

        String cleanTitle = requireText(title, "title");
        if (cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("title must have at most " + MAX_TITLE_LENGTH + " characters");
        }
        String cleanDescription = requireText(description, "description");
        if (!slaDeadline.isAfter(createdAt)) {
            throw new IllegalArgumentException("SLA deadline must be after the creation time");
        }

        return new Ticket(id, cleanTitle, cleanDescription, impact, requiredSkill, createdAt, slaDeadline, 0);
    }

    /**
     * Reconstruye un ticket ya existente sin volver a validar las transiciones que lo trajeron
     * hasta acá. Lo usa la capa de persistencia; el resto del código usa {@link #open}.
     */
    public static Ticket rebuild(TicketId id, String title, String description, Impact impact, Skill requiredSkill,
                                 Instant createdAt, Instant slaDeadline, TicketStatus status,
                                 EmployeeId assignee, String reviewReason, long version) {
        Ticket draft = open(id, title, description, impact, requiredSkill, createdAt, slaDeadline);
        Ticket ticket = new Ticket(draft.id, draft.title, draft.description, draft.impact,
                draft.requiredSkill, draft.createdAt, draft.slaDeadline, version);
        ticket.status = Objects.requireNonNull(status, "status is required");
        ticket.assignee = assignee;
        ticket.reviewReason = reviewReason;
        return ticket;
    }

    public void startEvaluation() {
        transitionTo(TicketStatus.EVALUANDO);
    }

    public void assignTo(EmployeeId employee) {
        Objects.requireNonNull(employee, "employee is required");
        transitionTo(TicketStatus.ASIGNADO);
        this.assignee = employee;
    }

    public void requireReview(String reason) {
        String cleanReason = requireText(reason, "review reason");
        transitionTo(TicketStatus.REVISION_REQUERIDA);
        this.reviewReason = cleanReason;
    }

    public void startWork() {
        transitionTo(TicketStatus.EN_PROGRESO);
    }

    public void resolve() {
        transitionTo(TicketStatus.RESUELTO);
    }

    public void close() {
        transitionTo(TicketStatus.CERRADO);
    }

    private void transitionTo(TicketStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidTicketTransitionException(status, next);
        }
        status = next;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public TicketId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Impact impact() {
        return impact;
    }

    public Skill requiredSkill() {
        return requiredSkill;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant slaDeadline() {
        return slaDeadline;
    }

    public long version() {
        return version;
    }

    /** Cuánto falta para que venza el SLA. Negativo si ya venció. */
    public Duration timeToSlaDeadline(Instant now) {
        return Duration.between(now, slaDeadline);
    }

    public TicketStatus status() {
        return status;
    }

    public Optional<EmployeeId> assignee() {
        return Optional.ofNullable(assignee);
    }

    public Optional<String> reviewReason() {
        return Optional.ofNullable(reviewReason);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Ticket that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

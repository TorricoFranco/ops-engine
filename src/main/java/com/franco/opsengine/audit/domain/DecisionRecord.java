package com.franco.opsengine.audit.domain;

import com.franco.opsengine.routing.domain.CandidateEvaluation;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Qué decidió el sistema sobre un ticket, y con qué.
 *
 * <p>Append-only: una decisión no se corrige, se agrega otra. Lo que la persona hace después
 * vive en el módulo de revisión, así queda claro qué decidió el sistema y qué decidió alguien.
 *
 * <p>No incluye el texto del ticket. Está en su tabla y copiarlo acá multiplicaría los datos
 * personales sin que eso agregue nada para reconstruir la decisión (RNF-04).
 *
 * @param judgement nulo cuando el proveedor no respondió: no hubo juicio que registrar
 */
public record DecisionRecord(
        UUID id,
        TicketId ticketId,
        String provider,
        String policyVersion,
        JudgementSnapshot judgement,
        Outcome outcome,
        EmployeeId assignedEmployee,
        ReviewReason reviewReason,
        String rationale,
        List<CandidateEvaluation> candidates,
        long latencyMs,
        Instant decidedAt) {

    public enum Outcome {
        ASSIGNED,
        SENT_TO_REVIEW
    }

    public DecisionRecord {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(ticketId, "ticket id is required");
        Objects.requireNonNull(outcome, "outcome is required");
        Objects.requireNonNull(decidedAt, "the time of the decision is required");
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("the provider is required to reproduce the decision");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("the policy version is required to reproduce the decision");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("a decision without a rationale cannot be explained");
        }
        if (outcome == Outcome.ASSIGNED && assignedEmployee == null) {
            throw new IllegalArgumentException("an assignment needs the employee it went to");
        }
        if (outcome == Outcome.SENT_TO_REVIEW && reviewReason == null) {
            throw new IllegalArgumentException("sending to review needs a reason");
        }
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latency cannot be negative");
        }
        candidates = List.copyOf(candidates);
    }

    public static DecisionRecord of(TicketId ticketId, String provider, JudgementSnapshot judgement,
                                    RoutingOutcome outcome, long latencyMs, Instant decidedAt) {
        return switch (outcome) {
            case RoutingOutcome.Assign assign -> new DecisionRecord(
                    UUID.randomUUID(), ticketId, provider, assign.policyVersion(), judgement,
                    Outcome.ASSIGNED, assign.employeeId(), null, assign.rationale(),
                    assign.candidates(), latencyMs, decidedAt);
            case RoutingOutcome.SendToReview review -> new DecisionRecord(
                    UUID.randomUUID(), ticketId, provider, review.policyVersion(), judgement,
                    Outcome.SENT_TO_REVIEW, null, review.reason(), review.rationale(),
                    review.candidates(), latencyMs, decidedAt);
        };
    }

    public Optional<JudgementSnapshot> judgementSnapshot() {
        return Optional.ofNullable(judgement);
    }

    public Optional<EmployeeId> assignee() {
        return Optional.ofNullable(assignedEmployee);
    }

    public Optional<ReviewReason> review() {
        return Optional.ofNullable(reviewReason);
    }

    /** Si la decisión llegó a tener una sugerencia que una persona pueda contrastar. */
    public boolean hasSuggestion() {
        return judgement != null;
    }
}

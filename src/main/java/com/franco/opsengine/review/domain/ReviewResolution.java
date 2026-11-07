package com.franco.opsengine.review.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Qué hizo una persona con un ticket que cayó en revisión.
 *
 * <p>Esto es lo que después permite comparar al sistema contra las personas. Por eso un
 * desacuerdo exige motivo: sin el motivo queda un número que dice que el sistema se equivocó,
 * pero no en qué, y con eso no se corrige nada.
 *
 * @param agreedWithSuggestion vacío cuando no hubo sugerencia que contrastar, que es el caso en
 *                             que el proveedor no respondió. No es lo mismo que un desacuerdo.
 */
public record ReviewResolution(
        UUID id,
        TicketId ticketId,
        UUID decisionRecordId,
        String resolvedBy,
        Resolution resolution,
        EmployeeId assignedEmployee,
        Boolean agreedWithSuggestion,
        String disagreementReason,
        Instant resolvedAt) {

    public enum Resolution {
        ASSIGNED,
        CLOSED
    }

    public ReviewResolution {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(ticketId, "ticket id is required");
        Objects.requireNonNull(resolution, "resolution is required");
        Objects.requireNonNull(resolvedAt, "the time of the resolution is required");
        if (resolvedBy == null || resolvedBy.isBlank()) {
            throw new IllegalArgumentException("the resolution needs to say who made it");
        }
        if (resolution == Resolution.ASSIGNED && assignedEmployee == null) {
            throw new IllegalArgumentException("assigning from review needs an employee");
        }
        if (resolution == Resolution.CLOSED && assignedEmployee != null) {
            throw new IllegalArgumentException("closing from review cannot name an assignee");
        }
        if (Boolean.FALSE.equals(agreedWithSuggestion) && isBlank(disagreementReason)) {
            throw new IllegalArgumentException("a disagreement without a reason cannot be acted on");
        }
        disagreementReason = isBlank(disagreementReason) ? null : disagreementReason.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Optional<EmployeeId> assignee() {
        return Optional.ofNullable(assignedEmployee);
    }

    public Optional<Boolean> agreement() {
        return Optional.ofNullable(agreedWithSuggestion);
    }

    public Optional<String> reasonForDisagreement() {
        return Optional.ofNullable(disagreementReason);
    }

    /** La persona contradijo lo que había sugerido el sistema. */
    public boolean isOverride() {
        return Boolean.FALSE.equals(agreedWithSuggestion);
    }
}

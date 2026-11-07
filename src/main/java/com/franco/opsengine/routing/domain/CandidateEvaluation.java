package com.franco.opsengine.routing.domain;

import com.franco.opsengine.shared.domain.EmployeeId;

import java.util.Objects;
import java.util.Optional;

/**
 * Qué pasó con un candidato. Los descartados se conservan con su motivo: sin esto la decisión
 * no se puede explicar, y explicarla es el punto del sistema.
 */
public record CandidateEvaluation(
        EmployeeId employeeId,
        String employeeName,
        boolean eligible,
        RejectionReason rejectionReason,
        int openTickets,
        int maxOpenTickets) {

    public CandidateEvaluation {
        Objects.requireNonNull(employeeId, "employee id is required");
        if (eligible && rejectionReason != null) {
            throw new IllegalArgumentException("an eligible candidate cannot carry a rejection reason");
        }
        if (!eligible && rejectionReason == null) {
            throw new IllegalArgumentException("a rejected candidate needs a reason");
        }
    }

    public static CandidateEvaluation eligible(EmployeeId id, String name, int openTickets, int maxOpenTickets) {
        return new CandidateEvaluation(id, name, true, null, openTickets, maxOpenTickets);
    }

    public static CandidateEvaluation rejected(EmployeeId id, String name, RejectionReason reason,
                                               int openTickets, int maxOpenTickets) {
        return new CandidateEvaluation(id, name, false, reason, openTickets, maxOpenTickets);
    }

    public Optional<RejectionReason> rejection() {
        return Optional.ofNullable(rejectionReason);
    }
}

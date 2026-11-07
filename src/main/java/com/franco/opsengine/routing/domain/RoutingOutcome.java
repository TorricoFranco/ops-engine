package com.franco.opsengine.routing.domain;

import com.franco.opsengine.shared.domain.EmployeeId;

import java.util.List;
import java.util.Objects;

/**
 * Resultado del ruteo. Siempre trae con qué versión de reglas se decidió y qué pasó con cada
 * candidato, incluso cuando el ticket termina en revisión.
 */
public sealed interface RoutingOutcome {

    String policyVersion();

    List<CandidateEvaluation> candidates();

    String rationale();

    record Assign(
            EmployeeId employeeId,
            String rationale,
            List<CandidateEvaluation> candidates,
            String policyVersion) implements RoutingOutcome {

        public Assign {
            Objects.requireNonNull(employeeId, "employee id is required");
            candidates = List.copyOf(candidates);
        }
    }

    record SendToReview(
            ReviewReason reason,
            String rationale,
            List<CandidateEvaluation> candidates,
            String policyVersion) implements RoutingOutcome {

        public SendToReview {
            Objects.requireNonNull(reason, "review reason is required");
            candidates = List.copyOf(candidates);
        }
    }
}

package com.franco.opsengine.evaluation.domain;

import com.franco.opsengine.shared.domain.EmployeeId;

import java.util.Optional;

/**
 * Una forma de decidir a quién va un ticket, para poder comparar varias sobre el mismo dataset.
 */
public interface EvaluationStrategy {

    String name();

    /**
     * @return vacío cuando la estrategia prefiere que lo mire una persona
     */
    Decision decide(LabelledTicket ticket, EvaluationRoster roster);

    /**
     * @param employeeId   a quién se asignó, vacío si va a revisión
     * @param reviewReason por qué va a revisión, vacío si se asignó
     */
    record Decision(EmployeeId employeeId, String reviewReason) {

        public static Decision assignTo(EmployeeId employeeId) {
            return new Decision(employeeId, null);
        }

        public static Decision review(String reason) {
            return new Decision(null, reason);
        }

        public Optional<EmployeeId> assignee() {
            return Optional.ofNullable(employeeId);
        }

        public boolean wasAssigned() {
            return employeeId != null;
        }
    }
}

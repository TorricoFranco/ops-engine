package com.franco.opsengine.evaluation.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Corre una estrategia sobre el dataset y cuenta cómo le fue.
 *
 * <p>Es una simulación en memoria: no toca la base ni crea tickets. La carga del equipo sí se
 * acumula entre casos, porque una estrategia que manda todo al mismo agente debería notarse.
 */
public final class Evaluator {

    private Evaluator() {
    }

    /** El detalle por caso, para poder mirar en qué se equivocó y no solo cuánto. */
    public record CaseResult(
            String ticketId,
            Skill expectedSkill,
            Skill actualSkill,
            String reviewReason,
            Outcome outcome) {

        public enum Outcome {
            CORRECT,
            SILENT_ERROR,
            SENT_TO_REVIEW
        }
    }

    public record Run(StrategyReport report, List<CaseResult> cases) {
    }

    public static Run run(EvaluationStrategy strategy, List<LabelledTicket> dataset,
                          EvaluationRoster roster) {
        int correct = 0;
        int silentErrors = 0;
        int sentToReview = 0;
        Map<String, Integer> byReviewReason = new LinkedHashMap<>();
        List<CaseResult> cases = new ArrayList<>();

        for (LabelledTicket ticket : dataset) {
            EvaluationStrategy.Decision decision = strategy.decide(ticket, roster);

            if (!decision.wasAssigned()) {
                sentToReview++;
                byReviewReason.merge(decision.reviewReason(), 1, Integer::sum);
                cases.add(new CaseResult(ticket.id(), ticket.expectedSkill(), null,
                        decision.reviewReason(), CaseResult.Outcome.SENT_TO_REVIEW));
                continue;
            }

            EmployeeId assignee = decision.employeeId();
            roster.recordAssignment(assignee);
            Skill actual = roster.skillOf(assignee).orElse(null);

            boolean hit = ticket.expectedSkill().equals(actual);
            if (hit) {
                correct++;
            } else {
                silentErrors++;
            }
            cases.add(new CaseResult(ticket.id(), ticket.expectedSkill(), actual, null,
                    hit ? CaseResult.Outcome.CORRECT : CaseResult.Outcome.SILENT_ERROR));
        }

        return new Run(
                new StrategyReport(strategy.name(), dataset.size(), correct, silentErrors,
                        sentToReview, byReviewReason),
                List.copyOf(cases));
    }
}

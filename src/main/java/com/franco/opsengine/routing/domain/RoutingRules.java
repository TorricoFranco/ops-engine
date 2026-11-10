package com.franco.opsengine.routing.domain;

import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.Skill;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Las reglas de asignación. Función pura: mismas entradas, mismo resultado, sin base de datos
 * ni red, así cada rama se prueba con un test unitario.
 *
 * <p>Acá es donde el juicio del modelo se cruza con los datos duros del sistema. El modelo
 * sugiere una competencia y estima cuánto le duele al cliente; la disponibilidad, la carga y
 * la habilitación las sabe la aplicación, y no se negocian. Una sugerencia nunca alcanza para
 * saltear una restricción.
 */
public final class RoutingRules {

    private RoutingRules() {
    }

    public static RoutingOutcome decide(TicketJudgement judgement,
                                        List<RoutingCandidate> candidates,
                                        Duration timeToSlaDeadline,
                                        RoutingPolicy policy) {

        // Se mira primero: sin entender el ticket, cualquier otra respuesta sería ruido.
        if (!judgement.enoughInformation().isYes(policy.minEnoughInformation())) {
            return new RoutingOutcome.RequestMoreInformation(
                    "el ticket no describe el problema con suficiente detalle (%.2f < %.2f)"
                            .formatted(judgement.enoughInformation().probability(),
                                    policy.minEnoughInformation()),
                    List.of(),
                    policy.version());
        }

        if (!judgement.skillRecognized().isYes(policy.minSkillRecognized())) {
            return review(ReviewReason.SKILL_NOT_RECOGNIZED, policy,
                    "el ticket no parece corresponder a ninguna competencia del equipo (%.2f < %.2f)"
                            .formatted(judgement.skillRecognized().probability(), policy.minSkillRecognized()),
                    List.of());
        }

        if (!judgement.requiredSkill().isAtLeast(policy.minSkillConfidence())) {
            return review(ReviewReason.LOW_CONFIDENCE, policy,
                    "la competencia del ticket no supera el umbral (%.2f < %.2f)".formatted(
                            judgement.requiredSkill().probabilityOf(judgement.requiredSkill().value()),
                            policy.minSkillConfidence()),
                    List.of());
        }

        Skill required = judgement.suggestedSkill();
        boolean critical = isCritical(judgement, timeToSlaDeadline, policy);
        List<CandidateEvaluation> evaluations = evaluate(candidates, required, critical);

        List<CandidateEvaluation> eligible = evaluations.stream()
                .filter(CandidateEvaluation::eligible)
                // Menos carga primero; con empate gana el id más chico, para que sea reproducible.
                .sorted(Comparator.comparingInt(CandidateEvaluation::openTickets)
                        .thenComparing(candidate -> candidate.employeeId().value()))
                .toList();

        if (eligible.isEmpty()) {
            return review(ReviewReason.NO_ELIGIBLE_AGENT, policy,
                    "ningún empleado cumple los requisitos para la competencia del ticket%s".formatted(
                            critical ? " con habilitación para incidentes críticos" : ""),
                    evaluations);
        }

        CandidateEvaluation chosen = eligible.getFirst();
        return new RoutingOutcome.Assign(
                chosen.employeeId(),
                "%s tiene la competencia del ticket y es quien menos carga tiene (%d de %d)%s".formatted(
                        chosen.employeeName(),
                        chosen.openTickets(), chosen.maxOpenTickets(),
                        critical ? ", y está habilitado para incidentes críticos" : ""),
                evaluations,
                policy.version());
    }

    /**
     * Un caso es crítico si el SLA está por vencer, o si el modelo ve urgencia alta junto con
     * una caída o varios clientes afectados.
     *
     * <p>El vencimiento real del SLA alcanza por sí solo (RF-05): es un dato del sistema y pesa
     * más que la urgencia que se desprende de cómo está escrito el reclamo.
     */
    static boolean isCritical(TicketJudgement judgement, Duration timeToSlaDeadline, RoutingPolicy policy) {
        if (timeToSlaDeadline.compareTo(policy.slaAtRiskWindow()) <= 0) {
            return true;
        }
        boolean urgent = judgement.urgency().normalized() >= policy.criticalUrgency();
        boolean widespread = judgement.serviceUnavailable().isYes(policy.criticalNoul())
                || judgement.multipleCustomersAffected().isYes(policy.criticalNoul());
        return urgent && widespread;
    }

    private static List<CandidateEvaluation> evaluate(List<RoutingCandidate> candidates, Skill required,
                                                      boolean critical) {
        List<CandidateEvaluation> evaluations = new ArrayList<>();
        for (RoutingCandidate candidate : candidates) {
            RejectionReason rejection = rejectionFor(candidate, required, critical);
            evaluations.add(rejection == null
                    ? CandidateEvaluation.eligible(candidate.id(), candidate.name(),
                            candidate.openTickets(), candidate.maxOpenTickets())
                    : CandidateEvaluation.rejected(candidate.id(), candidate.name(), rejection,
                            candidate.openTickets(), candidate.maxOpenTickets()));
        }
        return List.copyOf(evaluations);
    }

    /** El orden importa: se informa el primer motivo que descalifica al candidato. */
    private static RejectionReason rejectionFor(RoutingCandidate candidate, Skill required, boolean critical) {
        if (!candidate.skills().contains(required)) {
            return RejectionReason.MISSING_SKILL;
        }
        if (!candidate.available()) {
            return RejectionReason.UNAVAILABLE;
        }
        if (critical && !candidate.allowedForCriticalIncidents()) {
            return RejectionReason.INSUFFICIENT_CLEARANCE;
        }
        if (!candidate.hasCapacity()) {
            return RejectionReason.AT_CAPACITY;
        }
        return null;
    }

    private static RoutingOutcome review(ReviewReason reason, RoutingPolicy policy, String rationale,
                                         List<CandidateEvaluation> evaluations) {
        return new RoutingOutcome.SendToReview(reason, rationale, evaluations, policy.version());
    }
}

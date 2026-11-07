package com.franco.opsengine.audit.domain;

import com.franco.opsengine.decision.domain.TicketJudgement;

import java.util.Objects;

/**
 * Lo que devolvió el proveedor, congelado en números.
 *
 * <p>Se guardan las probabilidades y no solo la conclusión: sin ellas no se puede saber si una
 * decisión fue ajustada o clarísima, que es lo que hace falta para mover un umbral con criterio.
 */
public record JudgementSnapshot(
        String suggestedSkill,
        double skillConfidence,
        double skillRecognized,
        double urgency,
        int urgencyLevels,
        double multipleCustomersAffected,
        double serviceUnavailable) {

    public JudgementSnapshot {
        Objects.requireNonNull(suggestedSkill, "the suggested skill is required");
    }

    public static JudgementSnapshot of(TicketJudgement judgement) {
        return new JudgementSnapshot(
                judgement.requiredSkill().value(),
                judgement.requiredSkill().probabilityOf(judgement.requiredSkill().value()),
                judgement.skillRecognized().probability(),
                judgement.urgency().value(),
                judgement.urgency().levels(),
                judgement.multipleCustomersAffected().probability(),
                judgement.serviceUnavailable().probability());
    }
}

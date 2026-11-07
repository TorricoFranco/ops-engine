package com.franco.opsengine.review.application;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import org.springframework.stereotype.Service;

/**
 * Las cifras con las que se juzga al motor.
 *
 * <p>Se calculan sobre lo que hay guardado, no sobre contadores en memoria: así sobreviven a
 * un reinicio y se pueden recalcular. Son métricas de negocio, distintas de las de salud
 * técnica que expone Actuator (ver `docs/monitoring.md`).
 */
@Service
public class ReviewMetrics {

    private final DecisionRecordRepository decisions;
    private final ReviewResolutionRepository resolutions;

    public ReviewMetrics(DecisionRecordRepository decisions, ReviewResolutionRepository resolutions) {
        this.decisions = decisions;
        this.resolutions = resolutions;
    }

    /**
     * @param decisions        decisiones tomadas
     * @param assigned         cuántas terminaron en una asignación automática
     * @param sentToReview     cuántas quedaron esperando a una persona
     * @param reviewRate       proporción que necesitó una persona
     * @param resolutions      revisiones ya resueltas
     * @param comparable       resoluciones donde hubo una sugerencia que contrastar
     * @param overrides        de esas, en cuántas la persona contradijo al sistema
     * @param overrideRate     overrides sobre comparables. Vacío mientras no haya comparables:
     *                         un cero inventado se leería como que el motor nunca se equivoca
     */
    public record Snapshot(
            long decisions,
            long assigned,
            long sentToReview,
            Double reviewRate,
            long resolutions,
            long comparable,
            long overrides,
            Double overrideRate) {
    }

    public Snapshot snapshot() {
        long total = decisions.countAll();
        long assigned = decisions.countByOutcome(DecisionRecord.Outcome.ASSIGNED);
        long sentToReview = decisions.countByOutcome(DecisionRecord.Outcome.SENT_TO_REVIEW);
        long resolved = resolutions.countAll();
        long comparable = resolutions.countWithSuggestion();
        long overrides = resolutions.countOverrides();

        return new Snapshot(
                total,
                assigned,
                sentToReview,
                ratio(sentToReview, total),
                resolved,
                comparable,
                overrides,
                ratio(overrides, comparable));
    }

    private static Double ratio(long numerator, long denominator) {
        return denominator == 0 ? null : (double) numerator / denominator;
    }
}

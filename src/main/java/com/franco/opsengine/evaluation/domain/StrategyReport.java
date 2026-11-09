package com.franco.opsengine.evaluation.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Cómo le fue a una estrategia sobre el dataset.
 *
 * <p>La cifra que importa son los errores silenciosos: asignaciones a alguien que no tenía la
 * competencia correcta y que, además, nadie mandó a revisar. Un ticket mal ruteado que al
 * menos cayó en la bandeja lo arregla una persona; uno mal ruteado que pasó derecho no lo
 * descubre nadie hasta que el cliente vuelve a escribir.
 *
 * <p>Por eso una estrategia que manda todo a revisión no es buena: no comete errores
 * silenciosos, pero tampoco resuelve nada. Hay que leer las dos cifras juntas.
 *
 * @param correct       asignado a alguien con la competencia esperada
 * @param silentErrors  asignado a alguien sin la competencia esperada, sin pasar por revisión
 * @param sentToReview  quedó esperando a una persona
 * @param byReviewReason cuántos casos por cada motivo de revisión
 */
public record StrategyReport(
        String strategy,
        int total,
        int correct,
        int silentErrors,
        int sentToReview,
        Map<String, Integer> byReviewReason) {

    public StrategyReport {
        Objects.requireNonNull(strategy, "the strategy needs a name");
        if (total <= 0) {
            throw new IllegalArgumentException("an empty dataset cannot be evaluated");
        }
        if (correct + silentErrors + sentToReview != total) {
            throw new IllegalArgumentException(
                    "the outcomes do not add up to the dataset size: " + total);
        }
        byReviewReason = Map.copyOf(byReviewReason);
    }

    /** Aciertos sobre el total. Una estrategia que deriva todo a revisión la tiene en cero. */
    public double accuracy() {
        return (double) correct / total;
    }

    /** De lo que la estrategia se animó a asignar, cuánto acertó. */
    public double precisionWhenAssigned() {
        int assigned = correct + silentErrors;
        return assigned == 0 ? 0 : (double) correct / assigned;
    }

    public double reviewRate() {
        return (double) sentToReview / total;
    }

    public double silentErrorRate() {
        return (double) silentErrors / total;
    }
}

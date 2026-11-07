package com.franco.opsengine.decision.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Respuesta a una pregunta de tipo choice: la opción más probable y la distribución completa.
 *
 * <p>{@code confidence} mide qué tan concentrada está la distribución, no si la opción es
 * correcta. Las reglas de ruteo deciden con {@link #probabilityOf(String)} y sus propios
 * umbrales, no con la confianza sola.
 */
public record ChoiceAnswer(String value, double confidence, Map<String, Double> probabilities) {

    public ChoiceAnswer {
        Objects.requireNonNull(value, "choice value is required");
        Objects.requireNonNull(probabilities, "probabilities are required");
        if (probabilities.isEmpty()) {
            throw new IllegalArgumentException("a choice answer needs at least one option");
        }
        if (!probabilities.containsKey(value)) {
            throw new IllegalArgumentException("chosen option " + value + " is not among the probabilities");
        }
        Probabilities.requireInRange(confidence, "confidence");
        probabilities.forEach((option, p) -> Probabilities.requireInRange(p, "probability of " + option));
        probabilities = Map.copyOf(probabilities);
    }

    public double probabilityOf(String option) {
        return probabilities.getOrDefault(option, 0.0);
    }

    /** La opción elegida supera el umbral pedido. */
    public boolean isAtLeast(double threshold) {
        return probabilityOf(value) >= threshold;
    }
}

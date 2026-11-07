package com.franco.opsengine.decision.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Respuesta a una pregunta de tipo score: una posición en la escala de niveles.
 *
 * <p>El valor es el promedio ponderado de los índices, así que puede caer entre dos niveles y
 * tiende al centro cuando la distribución tiene dos picos. Por eso se conserva la distribución:
 * una regla que necesite distinguir esos casos mira {@link #probabilityOfLevel(int)}.
 */
public record ScoreAnswer(double value, int levels, Map<Integer, Double> probabilities) {

    public ScoreAnswer {
        Objects.requireNonNull(probabilities, "probabilities are required");
        if (levels < 2) {
            throw new IllegalArgumentException("a score needs at least two levels");
        }
        if (value < 0 || value > levels - 1) {
            throw new IllegalArgumentException("score " + value + " is outside the scale of " + levels + " levels");
        }
        probabilities.forEach((level, p) -> Probabilities.requireInRange(p, "probability of level " + level));
        probabilities = Map.copyOf(probabilities);
    }

    public double probabilityOfLevel(int level) {
        return probabilities.getOrDefault(level, 0.0);
    }

    /** El score normalizado entre 0 y 1, para combinarlo con otras señales. */
    public double normalized() {
        return value / (levels - 1);
    }
}

package com.franco.opsengine.decision.domain;

/**
 * Respuesta a una pregunta de tipo noul: la probabilidad de que la afirmación sea verdadera.
 *
 * <p>No es un booleano. Un valor cerca de 0,5 significa que el contexto no alcanza para
 * responder, y ese caso tiene su propia rama en las reglas: no se fuerza a sí ni a no.
 */
public record NoulAnswer(double probability) {

    /** Umbrales iniciales recomendados por la documentación de TypeSafe. Se ajustan con datos. */
    public static final double DEFAULT_YES = 0.7;
    public static final double DEFAULT_NO = 0.3;

    public NoulAnswer {
        Probabilities.requireInRange(probability, "noul probability");
    }

    public boolean isYes(double yesThreshold) {
        return probability >= yesThreshold;
    }

    public boolean isNo(double noThreshold) {
        return probability <= noThreshold;
    }

    /** Entre los dos umbrales: el contexto no resuelve la pregunta. */
    public boolean isUnsettled(double noThreshold, double yesThreshold) {
        if (noThreshold > yesThreshold) {
            throw new IllegalArgumentException("the no threshold cannot be above the yes threshold");
        }
        return probability > noThreshold && probability < yesThreshold;
    }
}

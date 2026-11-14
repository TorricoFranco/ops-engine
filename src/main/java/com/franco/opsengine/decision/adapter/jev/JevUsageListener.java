package com.franco.opsengine.decision.adapter.jev;

/**
 * Recibe los tokens de cada respuesta de Jev que llegó. La evaluación los suma para estimar el
 * costo; la aplicación todavía no los guarda por decisión.
 */
@FunctionalInterface
public interface JevUsageListener {

    JevUsageListener NONE = (inputTokens, outputTokens) -> {
    };

    void responded(int inputTokens, int outputTokens);
}

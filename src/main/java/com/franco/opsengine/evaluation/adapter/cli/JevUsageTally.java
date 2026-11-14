package com.franco.opsengine.evaluation.adapter.cli;

import com.franco.opsengine.decision.adapter.jev.JevUsageListener;

/**
 * Suma los tokens que Jev reporta en cada respuesta de la evaluación.
 *
 * <p>El costo sale de los tokens de entrada, que son los facturables; los de salida son gratis.
 * Es una estimación: la cifra que vale es el saldo de la cuenta de TypeSafe.
 */
final class JevUsageTally implements JevUsageListener {

    private long calls;
    private long inputTokens;
    private long outputTokens;

    @Override
    public void responded(int inputTokens, int outputTokens) {
        this.calls++;
        this.inputTokens += inputTokens;
        this.outputTokens += outputTokens;
    }

    long calls() {
        return calls;
    }

    long inputTokens() {
        return inputTokens;
    }

    long outputTokens() {
        return outputTokens;
    }

    double costUsd(double pricePerMillionInputTokens) {
        return inputTokens * pricePerMillionInputTokens / 1_000_000.0;
    }
}

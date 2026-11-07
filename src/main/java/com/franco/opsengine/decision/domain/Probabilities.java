package com.franco.opsengine.decision.domain;

final class Probabilities {

    private Probabilities() {
    }

    static void requireInRange(double value, String name) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be between 0 and 1, but was " + value);
        }
    }
}

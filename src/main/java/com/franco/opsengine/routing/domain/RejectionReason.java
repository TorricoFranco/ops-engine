package com.franco.opsengine.routing.domain;

/** Por qué un candidato quedó afuera. Se guarda para poder explicar la decisión. */
public enum RejectionReason {
    MISSING_SKILL,
    UNAVAILABLE,
    AT_CAPACITY,
    INSUFFICIENT_CLEARANCE
}

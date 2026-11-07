package com.franco.opsengine.routing.domain;

/** Por qué el ticket va a una persona en lugar de asignarse solo. */
public enum ReviewReason {
    /** El proveedor falló, tardó de más o devolvió algo que no se entiende (RF-07). */
    PROVIDER_UNAVAILABLE,
    /** El ticket no parece corresponder a ninguna competencia del equipo. */
    SKILL_NOT_RECOGNIZED,
    /** La competencia elegida no supera el umbral configurado. */
    LOW_CONFIDENCE,
    /** Ningún empleado cumple los requisitos. */
    NO_ELIGIBLE_AGENT
}

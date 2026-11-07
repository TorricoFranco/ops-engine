package com.franco.opsengine.decision.adapter.jev;

import java.util.Map;

/**
 * Respuesta de POST /v1/systemone.
 *
 * <p>Las respuestas llegan bajo la misma clave que se le dio a cada pregunta. Se deserializan
 * en un único record con todos los campos opcionales en vez de usar polimorfismo por el campo
 * {@code type}: el mapeo a los tipos del dominio ya valida que esté lo que corresponde, y así
 * un campo nuevo del proveedor no rompe la deserialización.
 *
 * <p>{@code usage} queda como mapa crudo: todavía no se modela, solo se registra para medir
 * costo por decisión (fase 7). Sus claves vienen en snake_case.
 */
record JevResponse(String model, Map<String, Answer> answers, Map<String, Integer> usage) {

    record Answer(
            String type,
            String choice,
            Double confidence,
            Double noul,
            Double score,
            Map<String, Double> probabilities,
            Map<String, String> legend) {
    }
}

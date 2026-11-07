package com.franco.opsengine.decision.adapter.jev;

import java.util.List;
import java.util.Map;

/**
 * Cuerpo de POST /v1/systemone. Varias preguntas van en un mismo pedido: comparten el estado,
 * que se lee y se cobra una sola vez, y el proveedor las responde en paralelo.
 */
record JevRequest(String model, Object state, Map<String, Question> questions) {

    /**
     * {@code criteria} cambia de forma según el tipo: un mapa de opciones para choice, una lista
     * ordenada de niveles para score, y un mapa con las claves true y false para noul.
     */
    record Question(String type, String instructions, Object criteria) {

        static Question choice(String instructions, Map<String, String> options) {
            return new Question("choice", instructions, options);
        }

        static Question score(String instructions, List<String> levels) {
            return new Question("score", instructions, levels);
        }

        static Question noul(String instructions, Map<String, String> meaning) {
            return new Question("noul", instructions, meaning);
        }
    }
}

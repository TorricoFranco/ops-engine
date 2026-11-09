package com.franco.opsengine.evaluation.application;

import com.franco.opsengine.evaluation.domain.EvaluationRoster;
import com.franco.opsengine.evaluation.domain.EvaluationStrategy;
import com.franco.opsengine.evaluation.domain.LabelledTicket;

/**
 * Línea base sin inteligencia: al que menos carga tenga, sin leer el ticket.
 *
 * <p>Está para dar un piso. Cualquier cosa que el motor haga tiene que ser mejor que esto, y
 * si no lo es, la respuesta honesta es que el motor no justifica su complejidad.
 */
public class LeastLoadedStrategy implements EvaluationStrategy {

    @Override
    public String name() {
        return "Reparto por carga (sin leer el ticket)";
    }

    @Override
    public Decision decide(LabelledTicket ticket, EvaluationRoster roster) {
        return roster.leastLoaded()
                .map(Decision::assignTo)
                .orElseGet(() -> Decision.review("NO_AGENTS"));
    }
}

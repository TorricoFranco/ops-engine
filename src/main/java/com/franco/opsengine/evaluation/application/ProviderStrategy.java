package com.franco.opsengine.evaluation.application;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.evaluation.domain.EvaluationRoster;
import com.franco.opsengine.evaluation.domain.EvaluationStrategy;
import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.routing.domain.RoutingPolicy;
import com.franco.opsengine.routing.domain.RoutingRules;
import com.franco.opsengine.shared.domain.Skill;

import java.time.Duration;
import java.util.Map;

/**
 * Un proveedor de juicios más las reglas de ruteo: exactamente lo que corre en producción.
 *
 * <p>Se usa con distintos proveedores para separar qué aporta cada parte. Con el proveedor
 * de palabras clave se mide cuánto alcanza con heurísticas; con Jev, cuánto suma el modelo
 * por encima de eso.
 *
 * <p>Todos los casos se evalúan con un SLA holgado. Si algunos estuvieran por vencer, las
 * reglas los volverían críticos y el resultado mediría la habilitación del equipo en lugar
 * de la elección de competencia.
 */
public class ProviderStrategy implements EvaluationStrategy {

    private static final Duration GENEROUS_SLA = Duration.ofHours(8);

    private final String label;
    private final DecisionProvider provider;
    private final Map<Skill, String> catalog;
    private final RoutingPolicy policy;

    public ProviderStrategy(String label, DecisionProvider provider,
                            Map<Skill, String> catalog, RoutingPolicy policy) {
        this.label = label;
        this.provider = provider;
        this.catalog = Map.copyOf(catalog);
        this.policy = policy;
    }

    @Override
    public String name() {
        return label;
    }

    @Override
    public Decision decide(LabelledTicket ticket, EvaluationRoster roster) {
        TicketJudgement judgement;
        try {
            judgement = provider.judge(new TicketContext(ticket.title(), ticket.description(), catalog));
        } catch (DecisionUnavailableException e) {
            // Igual que en producción: una falla no se convierte en un juicio.
            return Decision.review("PROVIDER_UNAVAILABLE");
        }

        RoutingOutcome outcome = RoutingRules.decide(
                judgement, roster.candidatesFor(judgement.suggestedSkill()), GENEROUS_SLA, policy);

        return switch (outcome) {
            case RoutingOutcome.Assign assign -> Decision.assignTo(assign.employeeId());
            case RoutingOutcome.SendToReview review -> Decision.review(review.reason().name());
        };
    }
}

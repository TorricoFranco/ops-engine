package com.franco.opsengine.routing.adapter.metrics;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.routing.application.RoutingMetrics;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class MicrometerRoutingMetrics implements RoutingMetrics {

    private final MeterRegistry registry;

    MicrometerRoutingMetrics(MeterRegistry registry, DecisionProvider provider) {
        this.registry = registry;
        preRegister(provider.name());
    }

    /**
     * Crea las series en cero al arrancar.
     *
     * <p>Micrometer registra un contador recién la primera vez que se incrementa, así que
     * hasta la primera decisión del día el panel no muestra una línea en cero: no muestra
     * nada, que se lee como que el tablero está roto. Además {@code rate()} necesita dos
     * puntos para dar un número, y sin el cero inicial el primer incremento no produce
     * ninguno.
     */
    private void preRegister(String provider) {
        decisions("assigned", "none");
        decisions("needs_more_information", "none");
        for (ReviewReason reason : ReviewReason.values()) {
            decisions("sent_to_review", reason.name());
        }
        judgementTimer(provider, "ok");
        judgementTimer(provider, "unavailable");
    }

    @Override
    public void judged(String provider, Duration took, boolean available) {
        judgementTimer(provider, available ? "ok" : "unavailable").record(took);
    }

    @Override
    public void decided(RoutingOutcome outcome) {
        decisions(name(outcome), reason(outcome)).increment();
    }

    private Timer judgementTimer(String provider, String result) {
        return Timer.builder("ops.decision.judgement")
                .description("Tiempo hasta tener el juicio del proveedor, reintentos incluidos")
                .tag("provider", provider)
                .tag("result", result)
                .register(registry);
    }

    /**
     * El motivo solo existe cuando el ticket va a revisión. En los demás casos la etiqueta
     * igual se emite con "none": una serie a la que a veces le falta una etiqueta no se puede
     * sumar en Prometheus, porque {@code sum by (outcome)} la deja afuera.
     */
    private Counter decisions(String outcome, String reason) {
        return Counter.builder("ops.routing.decisions")
                .description("Decisiones de ruteo por resultado")
                .tag("outcome", outcome)
                .tag("reason", reason)
                .register(registry);
    }

    @Override
    public void routingFailed(String reason) {
        Counter.builder("ops.routing.failures")
                .description("Intentos de ruteo que terminaron en error")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    private static String name(RoutingOutcome outcome) {
        return switch (outcome) {
            case RoutingOutcome.Assign ignored -> "assigned";
            case RoutingOutcome.SendToReview ignored -> "sent_to_review";
            case RoutingOutcome.RequestMoreInformation ignored -> "needs_more_information";
        };
    }

    private static String reason(RoutingOutcome outcome) {
        return outcome instanceof RoutingOutcome.SendToReview review ? review.reason().name() : "none";
    }
}

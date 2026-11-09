package com.franco.opsengine.routing.adapter.metrics;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.shared.domain.EmployeeId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerRoutingMetricsTest {

    private MeterRegistry registry;
    private MicrometerRoutingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MicrometerRoutingMetrics(registry, PROVIDER);
    }

    @Test
    void registersEverySeriesAtZeroBeforeTheFirstDecision() {
        // Un contador que todavía no se incrementó no existe para Micrometer, y un panel sin
        // serie no se ve como un cero: se ve como un tablero roto.
        assertThat(decisions("assigned")).isZero();
        assertThat(decisions("sent_to_review")).isZero();
        assertThat(decisions("needs_more_information")).isZero();
        assertThat(registry.get("ops.decision.judgement").tag("result", "ok").timer().count())
                .isZero();
    }

    @Test
    void countsAssignmentsSeparatelyFromReviews() {
        metrics.decided(assign());
        metrics.decided(assign());
        metrics.decided(review(ReviewReason.LOW_CONFIDENCE));

        assertThat(decisions("assigned")).isEqualTo(2);
        assertThat(decisions("sent_to_review")).isEqualTo(1);
    }

    @Test
    void labelsEveryDecisionWithAReasonEvenWhenThereIsNone() {
        // Una serie a la que a veces le falta una etiqueta no se puede sumar en Prometheus:
        // sum by (outcome) dejaría afuera justo las asignaciones.
        metrics.decided(assign());

        assertThat(registry.get("ops.routing.decisions").tag("outcome", "assigned")
                .counter().getId().getTag("reason")).isEqualTo("none");
    }

    @Test
    void keepsTheReviewReasonSoTheAlertCanTellThemApart() {
        // Derivar por poca confianza es parte del diseño; derivar porque el proveedor no
        // responde es una caída. Si comparten serie, la alerta no distingue una de la otra.
        metrics.decided(review(ReviewReason.PROVIDER_UNAVAILABLE));

        assertThat(registry.get("ops.routing.decisions")
                .tag("reason", ReviewReason.PROVIDER_UNAVAILABLE.name())
                .counter().count()).isEqualTo(1);
    }

    @Test
    void timesFailedJudgementsToo() {
        // Una caída que tarda diez segundos en fallar es un problema distinto de una que
        // falla al instante, y el promedio de las respuestas buenas no lo muestra.
        metrics.judged("jev", Duration.ofMillis(400), true);
        metrics.judged("jev", Duration.ofSeconds(10), false);

        assertThat(registry.get("ops.decision.judgement").tag("result", "ok").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("ops.decision.judgement").tag("result", "unavailable")
                .timer().totalTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(10);
    }

    @Test
    void separatesFailuresByReason() {
        metrics.routingFailed("optimistic_lock");

        assertThat(registry.get("ops.routing.failures").tag("reason", "optimistic_lock")
                .counter().count()).isEqualTo(1);
    }

    private double decisions(String outcome) {
        return registry.get("ops.routing.decisions").tag("outcome", outcome)
                .counters().stream().mapToDouble(counter -> counter.count()).sum();
    }

    /** Solo aporta el nombre: lo que se mide es el registro, no el juicio. */
    private static final DecisionProvider PROVIDER = new DecisionProvider() {

        @Override
        public TicketJudgement judge(TicketContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            return "jev";
        }
    };

    private static RoutingOutcome assign() {
        return new RoutingOutcome.Assign(
                new EmployeeId(UUID.randomUUID()), "motivo", List.of(), "test.1");
    }

    private static RoutingOutcome review(ReviewReason reason) {
        return new RoutingOutcome.SendToReview(reason, "motivo", List.of(), "test.1");
    }
}

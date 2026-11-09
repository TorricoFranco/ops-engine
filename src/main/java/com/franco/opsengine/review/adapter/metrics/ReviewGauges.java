package com.franco.opsengine.review.adapter.metrics;

import com.franco.opsengine.review.application.ReviewMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.ToDoubleFunction;

/**
 * Expone a Prometheus las cifras de negocio que ya calcula {@link ReviewMetrics}.
 *
 * <p>Son acumuladas sobre lo guardado, no contadores en memoria: sobreviven a un reinicio,
 * que es justamente lo que un contador de Micrometer no hace.
 */
@Component
class ReviewGauges {

    private static final Logger log = LoggerFactory.getLogger(ReviewGauges.class);

    private final ReviewMetrics metrics;

    ReviewGauges(ReviewMetrics metrics, MeterRegistry registry) {
        this.metrics = metrics;

        gauge(registry, "ops.review.resolutions", "Revisiones humanas resueltas",
                snapshot -> snapshot.resolutions());
        gauge(registry, "ops.review.comparable",
                "Resoluciones donde había una sugerencia que contrastar",
                snapshot -> snapshot.comparable());
        gauge(registry, "ops.review.overrides",
                "Resoluciones donde la persona contradijo al sistema",
                snapshot -> snapshot.overrides());
    }

    /**
     * La tasa de override no se publica como métrica propia a propósito: es un cociente, y
     * mientras no haya comparables no vale cero sino nada. Prometheus no sabe expresar "sin
     * dato", así que la división la hace el panel y queda vacío cuando el divisor es cero.
     */
    private void gauge(MeterRegistry registry, String name, String description,
                       ToDoubleFunction<ReviewMetrics.Snapshot> value) {
        Gauge.builder(name, this, self -> self.read(value))
                .description(description)
                .register(registry);
    }

    private double read(ToDoubleFunction<ReviewMetrics.Snapshot> value) {
        try {
            return value.applyAsDouble(metrics.snapshot());
        } catch (RuntimeException e) {
            log.warn("No se pudieron leer las métricas de revisión: {}", e.getMessage());
            return Double.NaN;
        }
    }
}

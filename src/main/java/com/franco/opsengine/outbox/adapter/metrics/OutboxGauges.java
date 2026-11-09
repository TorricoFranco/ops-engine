package com.franco.opsengine.outbox.adapter.metrics;

import com.franco.opsengine.outbox.application.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El atraso del outbox.
 *
 * <p>Es la métrica que avisa que las asignaciones se están guardando pero sus eventos no
 * salen: el sistema parece sano desde la API y por dentro dejó de publicar.
 */
@Component
class OutboxGauges {

    private static final Logger log = LoggerFactory.getLogger(OutboxGauges.class);

    private final OutboxRepository outbox;

    OutboxGauges(OutboxRepository outbox, MeterRegistry registry) {
        this.outbox = outbox;

        Gauge.builder("ops.outbox.pending", this, OutboxGauges::pending)
                .description("Eventos del outbox todavía sin publicar")
                .register(registry);
    }

    private double pending() {
        try {
            return outbox.countPending();
        } catch (RuntimeException e) {
            log.warn("No se pudo contar el outbox pendiente: {}", e.getMessage());
            return Double.NaN;
        }
    }
}

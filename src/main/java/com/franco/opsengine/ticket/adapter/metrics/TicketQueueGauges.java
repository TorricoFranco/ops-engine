package com.franco.opsengine.ticket.adapter.metrics;

import com.franco.opsengine.ticket.application.TicketQueueStats;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

/**
 * Publica el estado de la cola como gauges.
 *
 * <p>Las cinco cifras se leen de una sola consulta y se guardan: si cada gauge fuera a la
 * base por su cuenta, un mismo scrape podría mezclar instantes distintos.
 *
 * <p>La consulta ocurre durante el scrape, no en un hilo de fondo. Con este volumen son
 * contadores sobre índices y no vale la pena un refresco programado; si la tabla creciera,
 * habría que pasar a una vista materializada antes que a un caché en memoria.
 */
@Component
class TicketQueueGauges {

    private static final Logger log = LoggerFactory.getLogger(TicketQueueGauges.class);

    private final TicketQueueStats stats;
    private final Clock clock;
    private final Duration slaRiskWindow;
    private final AtomicReference<Reading> last = new AtomicReference<>(Reading.EMPTY);

    TicketQueueGauges(TicketQueueStats stats, Clock clock, MeterRegistry registry,
                      @Value("${app.routing.policy.sla-at-risk-window:PT30M}") Duration slaRiskWindow) {
        this.stats = stats;
        this.clock = clock;
        this.slaRiskWindow = slaRiskWindow;

        gauge(registry, "ops.tickets.pending", "Tickets creados que todavía no se rutearon",
                snapshot -> snapshot.pending());
        gauge(registry, "ops.tickets.pending.oldest.seconds",
                "Antigüedad del ticket sin rutear más viejo",
                snapshot -> snapshot.oldestPendingAge().toSeconds());
        gauge(registry, "ops.tickets.sla.at.risk",
                "Tickets que vencen dentro de la ventana y siguen sin resolverse",
                snapshot -> snapshot.slaAtRisk());
        gauge(registry, "ops.tickets.awaiting.review", "Tickets esperando a una persona",
                snapshot -> snapshot.awaitingReview());
        gauge(registry, "ops.tickets.awaiting.information",
                "Tickets esperando datos del cliente",
                snapshot -> snapshot.awaitingInformation());
    }

    private void gauge(MeterRegistry registry, String name, String description,
                       ToDoubleFunction<TicketQueueStats.Snapshot> value) {
        Gauge.builder(name, this, self -> value.applyAsDouble(self.read()))
                .description(description)
                .register(registry);
    }

    /**
     * Una sola consulta por scrape, compartida por los cinco gauges. Micrometer los lee uno
     * tras otro, así que el primero que llega consulta y el resto reutiliza esa lectura.
     */
    private TicketQueueStats.Snapshot read() {
        long now = System.nanoTime();
        Reading cached = last.get();
        if (cached.snapshot() != null && now - cached.takenAt() < FRESH_NANOS) {
            return cached.snapshot();
        }

        try {
            TicketQueueStats.Snapshot fresh = stats.snapshot(clock.instant(), slaRiskWindow);
            last.set(new Reading(fresh, now));
            return fresh;
        } catch (RuntimeException e) {
            // Que falle el monitoreo no puede tumbar el scrape entero. Se devuelve la última
            // lectura buena y queda el log; la base caída ya se ve en el health check.
            log.warn("No se pudo leer el estado de la cola para las métricas: {}", e.getMessage());
            return cached.snapshot() == null ? EMPTY_SNAPSHOT : cached.snapshot();
        }
    }

    /** Medio segundo: cubre un scrape completo y no alcanza para envejecer entre dos. */
    private static final long FRESH_NANOS = Duration.ofMillis(500).toNanos();

    private static final TicketQueueStats.Snapshot EMPTY_SNAPSHOT =
            new TicketQueueStats.Snapshot(0, Duration.ZERO, 0, 0, 0);

    private record Reading(TicketQueueStats.Snapshot snapshot, long takenAt) {

        static final Reading EMPTY = new Reading(null, 0);
    }
}

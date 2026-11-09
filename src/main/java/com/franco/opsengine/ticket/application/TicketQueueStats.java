package com.franco.opsengine.ticket.application;

import java.time.Duration;
import java.time.Instant;

/**
 * Cifras de la cola de tickets, para monitoreo.
 *
 * <p>Son puertos aparte de {@link TicketRepository} porque no se resuelven trayendo tickets:
 * se cuentan en la base. Calcularlas en memoria obligaría a traer la tabla entera en cada
 * scrape de Prometheus.
 */
public interface TicketQueueStats {

    /**
     * @param pending               tickets que todavía no se rutearon
     * @param oldestPendingAge      cuánto lleva esperando el más viejo. Cero si no hay ninguno
     * @param slaAtRisk             tickets que vencen dentro de la ventana y siguen sin resolverse
     * @param awaitingReview        tickets esperando a una persona
     * @param awaitingInformation   tickets esperando una respuesta del cliente
     */
    record Snapshot(
            long pending,
            Duration oldestPendingAge,
            long slaAtRisk,
            long awaitingReview,
            long awaitingInformation) {
    }

    Snapshot snapshot(Instant now, Duration slaRiskWindow);
}

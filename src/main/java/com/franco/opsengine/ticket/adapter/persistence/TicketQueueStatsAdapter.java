package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.ticket.application.TicketQueueStats;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Repository
class TicketQueueStatsAdapter implements TicketQueueStats {

    private final TicketJpaRepository jpa;

    TicketQueueStatsAdapter(TicketJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * De solo lectura y en una sola transacción: así las cinco cifras describen el mismo
     * instante. Sin eso, un panel podría mostrar cero pendientes y a la vez una antigüedad
     * del más viejo, que es una contradicción que haría dudar del tablero entero.
     */
    @Override
    @Transactional(readOnly = true)
    public Snapshot snapshot(Instant now, Duration slaRiskWindow) {
        Instant oldest = jpa.oldestCreatedAtByStatus(TicketStatus.NUEVO);

        return new Snapshot(
                jpa.countByStatus(TicketStatus.NUEVO),
                oldest == null ? Duration.ZERO : Duration.between(oldest, now),
                jpa.countAtRisk(now.plus(slaRiskWindow)),
                jpa.countByStatus(TicketStatus.REVISION_REQUERIDA),
                jpa.countByStatus(TicketStatus.INFORMACION_INSUFICIENTE));
    }
}

package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.ticket.domain.TicketStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID> {

    Optional<TicketEntity> findByIdempotencyKey(String idempotencyKey);

    List<TicketEntity> findByStatusOrderByCreatedAtAsc(TicketStatus status, Limit limit);

    long countByStatus(TicketStatus status);

    /** Null cuando no hay ninguno en ese estado. */
    @Query("select min(t.createdAt) from TicketEntity t where t.status = :status")
    Instant oldestCreatedAtByStatus(@Param("status") TicketStatus status);

    /**
     * Vence antes del límite y todavía no llegó a destino. Los estados terminales quedan
     * afuera: un ticket resuelto no tiene un SLA en riesgo aunque su fecha ya haya pasado.
     */
    @Query("""
            select count(t) from TicketEntity t
            where t.slaDeadline < :deadline
              and t.status not in (
                com.franco.opsengine.ticket.domain.TicketStatus.RESUELTO,
                com.franco.opsengine.ticket.domain.TicketStatus.CERRADO,
                com.franco.opsengine.ticket.domain.TicketStatus.ASIGNADO,
                com.franco.opsengine.ticket.domain.TicketStatus.EN_PROGRESO)
            """)
    long countAtRisk(@Param("deadline") Instant deadline);
}

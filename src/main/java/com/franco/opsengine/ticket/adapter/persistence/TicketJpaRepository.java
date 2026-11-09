package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.ticket.domain.TicketStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID> {

    Optional<TicketEntity> findByIdempotencyKey(String idempotencyKey);

    List<TicketEntity> findByStatusOrderByCreatedAtAsc(TicketStatus status, Limit limit);
}

package com.franco.opsengine.ticket.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID> {

    Optional<TicketEntity> findByIdempotencyKey(String idempotencyKey);
}

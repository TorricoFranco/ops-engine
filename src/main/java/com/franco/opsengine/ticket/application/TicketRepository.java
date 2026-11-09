package com.franco.opsengine.ticket.application;

import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.ticket.domain.TicketStatus;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de tickets. La implementación vive en el adapter y el dominio
 * no conoce JPA.
 */
public interface TicketRepository {

    Ticket save(Ticket ticket, String idempotencyKey);

    Optional<Ticket> findById(TicketId id);

    Optional<Ticket> findByIdempotencyKey(String idempotencyKey);

    List<Ticket> findAll();

    /** Los más viejos primero: el que lleva más esperando se atiende antes. */
    List<Ticket> findByStatus(TicketStatus status, int limit);
}

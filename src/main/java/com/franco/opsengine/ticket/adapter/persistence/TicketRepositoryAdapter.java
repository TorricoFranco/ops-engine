package com.franco.opsengine.ticket.adapter.persistence;

import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.springframework.data.domain.Limit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class TicketRepositoryAdapter implements TicketRepository {

    private final TicketJpaRepository jpa;

    TicketRepositoryAdapter(TicketJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Ticket save(Ticket ticket, String idempotencyKey) {
        try {
            return TicketMapper.toDomain(jpa.saveAndFlush(TicketMapper.toEntity(ticket, idempotencyKey)));
        } catch (DataIntegrityViolationException e) {
            // Dos pedidos con la misma clave llegaron a la vez: gana el primero y el segundo
            // devuelve ese mismo ticket en lugar de fallar.
            if (idempotencyKey == null) {
                throw e;
            }
            return jpa.findByIdempotencyKey(idempotencyKey)
                    .map(TicketMapper::toDomain)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<Ticket> findById(TicketId id) {
        return jpa.findById(id.value()).map(TicketMapper::toDomain);
    }

    @Override
    public Optional<Ticket> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(TicketMapper::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return jpa.findAll().stream().map(TicketMapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status, int limit) {
        return jpa.findByStatusOrderByCreatedAtAsc(status, Limit.of(limit)).stream()
                .map(TicketMapper::toDomain)
                .toList();
    }
}

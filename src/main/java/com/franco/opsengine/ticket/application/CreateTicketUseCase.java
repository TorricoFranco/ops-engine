package com.franco.opsengine.ticket.application;

import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * RF-01: crea un ticket. Si llega una Idempotency-Key ya usada, devuelve el ticket original
 * en lugar de crear uno nuevo.
 */
@Service
public class CreateTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTicketUseCase.class);

    private final TicketRepository tickets;
    private final Clock clock;

    public CreateTicketUseCase(TicketRepository tickets, Clock clock) {
        this.tickets = tickets;
        this.clock = clock;
    }

    public Ticket execute(CreateTicketCommand command) {
        String key = normalize(command.idempotencyKey());

        if (key != null) {
            var existing = tickets.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                log.info("Idempotency key reused, returning existing ticket ticketId={}",
                        existing.get().id().value());
                return existing.get();
            }
        }

        if (command.slaDuration() == null || command.slaDuration().isNegative() || command.slaDuration().isZero()) {
            throw new IllegalArgumentException("SLA duration must be positive");
        }

        Instant now = clock.instant();
        Ticket ticket = Ticket.open(
                TicketId.newId(),
                command.title(),
                command.description(),
                command.impact(),
                command.requiredSkill(),
                now,
                now.plus(command.slaDuration()));

        Ticket saved = tickets.save(ticket, key);
        log.info("Ticket created ticketId={} impact={} skill={}",
                saved.id().value(), saved.impact(), saved.requiredSkill().code());
        return saved;
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim();
    }
}

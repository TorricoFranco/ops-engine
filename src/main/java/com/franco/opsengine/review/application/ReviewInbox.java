package com.franco.opsengine.review.application;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Los tickets que están esperando a una persona, con la decisión que los dejó ahí.
 */
@Service
public class ReviewInbox {

    private final TicketRepository tickets;
    private final DecisionRecordRepository decisions;

    public ReviewInbox(TicketRepository tickets, DecisionRecordRepository decisions) {
        this.tickets = tickets;
        this.decisions = decisions;
    }

    public record PendingReview(Ticket ticket, DecisionRecord decision) {

        public Optional<DecisionRecord> lastDecision() {
            return Optional.ofNullable(decision);
        }
    }

    public List<PendingReview> pending() {
        return tickets.findAll().stream()
                .filter(ticket -> ticket.status() == TicketStatus.REVISION_REQUERIDA)
                .sorted(java.util.Comparator.comparing(Ticket::slaDeadline))
                .map(ticket -> new PendingReview(
                        ticket, decisions.findLatestByTicket(ticket.id()).orElse(null)))
                .toList();
    }
}

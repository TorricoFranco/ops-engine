package com.franco.opsengine.review.application;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.employee.application.EmployeeNotFoundException;
import com.franco.opsengine.employee.application.EmployeeRepository;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.outbox.application.OutboxRepository;
import com.franco.opsengine.outbox.domain.OutboxEvent;
import com.franco.opsengine.review.domain.ReviewResolution;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.application.TicketNotFoundException;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * RF-08: lo que hace un supervisor con un ticket que quedó en revisión.
 *
 * <p>Al resolver se registra si estuvo de acuerdo con lo que sugirió el sistema y, cuando no,
 * por qué. Ese registro es lo que después permite medir en qué se equivoca el motor.
 */
@Service
public class ResolveReviewUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveReviewUseCase.class);

    public static final String TICKET_REVIEW_RESOLVED = "ticket.review-resolved";

    private final TicketRepository tickets;
    private final EmployeeRepository employees;
    private final DecisionRecordRepository decisions;
    private final ReviewResolutionRepository resolutions;
    private final OutboxRepository outbox;
    private final Clock clock;

    public ResolveReviewUseCase(TicketRepository tickets, EmployeeRepository employees,
                                DecisionRecordRepository decisions,
                                ReviewResolutionRepository resolutions,
                                OutboxRepository outbox, Clock clock) {
        this.tickets = tickets;
        this.employees = employees;
        this.decisions = decisions;
        this.resolutions = resolutions;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public ReviewResolution assign(TicketId ticketId, EmployeeId employeeId, String resolvedBy,
                                   Boolean agreedWithSuggestion, String disagreementReason) {
        Ticket ticket = load(ticketId);
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        // La persona elige, pero las restricciones siguen valiendo: el aggregate decide si
        // este empleado puede tomar el ticket.
        employee.takeTicket(ticket.requiredSkill());
        ticket.assignTo(employeeId);

        employees.save(employee);
        tickets.save(ticket, null);

        return record(ticket, ReviewResolution.Resolution.ASSIGNED, employeeId, resolvedBy,
                agreedWithSuggestion, disagreementReason);
    }

    @Transactional
    public ReviewResolution close(TicketId ticketId, String resolvedBy,
                                  Boolean agreedWithSuggestion, String disagreementReason) {
        Ticket ticket = load(ticketId);
        ticket.close();
        tickets.save(ticket, null);

        return record(ticket, ReviewResolution.Resolution.CLOSED, null, resolvedBy,
                agreedWithSuggestion, disagreementReason);
    }

    private Ticket load(TicketId ticketId) {
        return tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private ReviewResolution record(Ticket ticket, ReviewResolution.Resolution kind,
                                    EmployeeId employeeId, String resolvedBy,
                                    Boolean agreedWithSuggestion, String disagreementReason) {
        DecisionRecord decision = decisions.findLatestByTicket(ticket.id()).orElse(null);

        // Sin sugerencia no hay nada que contrastar: se guarda vacío en lugar de contarlo como
        // acuerdo o desacuerdo, para no ensuciar la tasa de override.
        Boolean agreement = decision != null && decision.hasSuggestion() ? agreedWithSuggestion : null;
        UUID decisionId = decision == null ? null : decision.id();

        Instant now = clock.instant();
        ReviewResolution resolution = new ReviewResolution(
                UUID.randomUUID(), ticket.id(), decisionId, resolvedBy, kind, employeeId,
                agreement, agreedWithSuggestion == null ? null : disagreementReason, now);

        resolutions.record(resolution);
        outbox.record(OutboxEvent.of(TICKET_REVIEW_RESOLVED, ticket.id().value(),
                """
                {"ticketId":"%s","resolution":"%s","override":%s}"""
                        .formatted(ticket.id().value(), kind, resolution.isOverride()), now));

        log.info("Review resolved ticketId={} resolution={} override={} by={}",
                ticket.id().value(), kind, resolution.isOverride(), resolvedBy);
        return resolution;
    }
}

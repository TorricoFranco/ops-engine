package com.franco.opsengine.routing.application;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.employee.application.EmployeeRepository;
import com.franco.opsengine.employee.domain.Availability;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.outbox.application.OutboxRepository;
import com.franco.opsengine.outbox.domain.OutboxEvent;
import com.franco.opsengine.routing.domain.RoutingCandidate;
import com.franco.opsengine.routing.domain.RoutingOutcome;
import com.franco.opsengine.routing.domain.RoutingPolicy;
import com.franco.opsengine.routing.domain.RoutingRules;
import com.franco.opsengine.routing.domain.ReviewReason;
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
import java.util.List;

/**
 * Rutea un ticket: pide el juicio, aplica las reglas y guarda el resultado.
 *
 * <p>Todo ocurre en una transacción, incluido el evento del outbox. Si el guardado falla, el
 * evento tampoco queda: no puede haber una asignación sin su evento.
 *
 * <p>La llamada al proveedor queda dentro de la transacción por simplicidad. Es aceptable
 * mientras el timeout esté acotado (10 s por defecto); si el volumen crece, el paso de
 * decisión debería salir de la transacción.
 */
@Service
public class RouteTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(RouteTicketUseCase.class);

    private final TicketRepository tickets;
    private final EmployeeRepository employees;
    private final DecisionProvider decisions;
    private final SkillCatalog skills;
    private final OutboxRepository outbox;
    private final RoutingPolicy policy;
    private final Clock clock;

    public RouteTicketUseCase(TicketRepository tickets, EmployeeRepository employees,
                              DecisionProvider decisions, SkillCatalog skills,
                              OutboxRepository outbox, RoutingPolicy policy, Clock clock) {
        this.tickets = tickets;
        this.employees = employees;
        this.decisions = decisions;
        this.skills = skills;
        this.outbox = outbox;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public RoutingOutcome route(TicketId ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        ticket.startEvaluation();

        Instant now = clock.instant();
        RoutingOutcome outcome = judgeAndDecide(ticket, now);

        switch (outcome) {
            case RoutingOutcome.Assign assign -> applyAssignment(ticket, assign, now);
            case RoutingOutcome.SendToReview review -> applyReview(ticket, review, now);
        }
        return outcome;
    }

    private RoutingOutcome judgeAndDecide(Ticket ticket, Instant now) {
        TicketJudgement judgement;
        try {
            judgement = decisions.judge(new TicketContext(ticket.title(), ticket.description(), skills.all()));
        } catch (DecisionUnavailableException e) {
            // RF-07: la falla no se convierte en un juicio. El ticket queda esperando a una
            // persona, y no se pierde.
            log.warn("Decision provider unavailable, sending the ticket to review ticketId={} reason={}",
                    ticket.id().value(), e.getMessage());
            return new RoutingOutcome.SendToReview(
                    ReviewReason.PROVIDER_UNAVAILABLE,
                    "el proveedor de decisiones no respondió: " + e.getMessage(),
                    List.of(),
                    policy.version());
        }

        List<RoutingCandidate> candidates = employees.findBySkill(judgement.suggestedSkill()).stream()
                .map(RouteTicketUseCase::toCandidate)
                .toList();

        return RoutingRules.decide(judgement, candidates, ticket.timeToSlaDeadline(now), policy);
    }

    private void applyAssignment(Ticket ticket, RoutingOutcome.Assign assign, Instant now) {
        Employee employee = employees.findById(assign.employeeId())
                .orElseThrow(() -> new IllegalStateException(
                        "el empleado elegido ya no existe: " + assign.employeeId().value()));

        // Vuelve a validar sobre el aggregate: entre la lectura de candidatos y este punto la
        // carga pudo cambiar. El optimistic locking cubre el resto.
        employee.takeTicket(ticket.requiredSkill());
        ticket.assignTo(assign.employeeId());

        employees.save(employee);
        tickets.save(ticket, null);
        outbox.record(OutboxEvent.of(RoutingEvents.TICKET_ASSIGNED, ticket.id().value(),
                assignedPayload(ticket, assign), now));

        log.info("Ticket assigned ticketId={} employeeId={} policyVersion={}",
                ticket.id().value(), assign.employeeId().value(), assign.policyVersion());
    }

    private void applyReview(Ticket ticket, RoutingOutcome.SendToReview review, Instant now) {
        ticket.requireReview(review.reason() + ": " + review.rationale());
        tickets.save(ticket, null);
        outbox.record(OutboxEvent.of(RoutingEvents.TICKET_SENT_TO_REVIEW, ticket.id().value(),
                reviewPayload(ticket, review), now));

        log.info("Ticket sent to review ticketId={} reason={} policyVersion={}",
                ticket.id().value(), review.reason(), review.policyVersion());
    }

    private static RoutingCandidate toCandidate(Employee employee) {
        return new RoutingCandidate(
                employee.id(),
                employee.name(),
                employee.skills(),
                employee.availability() == Availability.AVAILABLE,
                employee.openTickets(),
                employee.maxOpenTickets(),
                employee.clearance().allowsCriticalIncidents());
    }

    /** JSON a mano: el payload es de tres campos y no justifica un serializador. */
    private static String assignedPayload(Ticket ticket, RoutingOutcome.Assign assign) {
        return """
                {"ticketId":"%s","employeeId":"%s","policyVersion":"%s"}"""
                .formatted(ticket.id().value(), assign.employeeId().value(), assign.policyVersion());
    }

    private static String reviewPayload(Ticket ticket, RoutingOutcome.SendToReview review) {
        return """
                {"ticketId":"%s","reason":"%s","policyVersion":"%s"}"""
                .formatted(ticket.id().value(), review.reason(), review.policyVersion());
    }
}

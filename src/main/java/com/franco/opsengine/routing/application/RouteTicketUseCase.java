package com.franco.opsengine.routing.application;

import com.franco.opsengine.audit.application.DecisionRecordRepository;
import com.franco.opsengine.audit.domain.DecisionRecord;
import com.franco.opsengine.audit.domain.JudgementSnapshot;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rutea un ticket: pide el juicio, aplica las reglas y guarda el resultado.
 *
 * <p>El juicio se pide fuera de la transacción y la escritura ocurre dentro de una sola,
 * evento del outbox incluido: si el guardado falla, el evento tampoco queda, porque no puede
 * haber una asignación sin su evento (ADR-010).
 */
@Service
public class RouteTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(RouteTicketUseCase.class);

    private final TicketRepository tickets;
    private final EmployeeRepository employees;
    private final DecisionProvider decisions;
    private final SkillCatalog skills;
    private final OutboxRepository outbox;
    private final DecisionRecordRepository decisionLog;
    private final RoutingPolicy policy;
    private final Clock clock;
    private final RoutingMetrics metrics;
    private final TransactionTemplate transactions;

    public RouteTicketUseCase(TicketRepository tickets, EmployeeRepository employees,
                              DecisionProvider decisions, SkillCatalog skills,
                              OutboxRepository outbox, DecisionRecordRepository decisionLog,
                              RoutingPolicy policy, Clock clock, RoutingMetrics metrics,
                              PlatformTransactionManager transactionManager) {
        this.tickets = tickets;
        this.employees = employees;
        this.decisions = decisions;
        this.skills = skills;
        this.outbox = outbox;
        this.decisionLog = decisionLog;
        this.policy = policy;
        this.clock = clock;
        this.metrics = metrics;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * Deliberadamente sin {@code @Transactional}.
     *
     * <p>El juicio tarda entre 350 ms y el timeout, y pedirlo dentro de una transacción
     * mantendría tomada una conexión de la base todo ese tiempo. Con volumen eso agota el
     * pool: la base no estaría ocupada, solo esperando a un tercero.
     *
     * <p>Así que primero se lee, después se consulta al proveedor sin transacción, y recién
     * al final se abre una para escribir. Si entre medio alguien más tocó el ticket, el
     * optimistic locking hace fallar la escritura y este intento se descarta (ADR-010).
     */
    public RoutingOutcome route(TicketId ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));

        Instant now = clock.instant();
        long startedAt = System.nanoTime();
        Decided decided = judgeAndDecide(ticket, now);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        // La transacción se abre acá y no antes, con el juicio ya en la mano. Es un template
        // y no una anotación a propósito: anotar un método privado o llamado desde la misma
        // clase no hace nada, porque el proxy de Spring no se entera.
        RoutingOutcome outcome = transactions.execute(status -> apply(ticketId, decided, latencyMs, now));

        // Se cuenta después de escribir. Contar antes inflaría la tasa de asignación con
        // intentos que el optimistic locking terminó descartando.
        metrics.decided(outcome);
        return outcome;
    }

    /**
     * Escribe el resultado. Vuelve a leer el ticket para trabajar sobre su versión actual:
     * entre la lectura inicial y este punto pasó una llamada a la red.
     */
    private RoutingOutcome apply(TicketId ticketId, Decided decided, long latencyMs, Instant now) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        ticket.startEvaluation();

        RoutingOutcome outcome = decided.outcome();
        switch (outcome) {
            case RoutingOutcome.Assign assign -> applyAssignment(ticket, assign, now);
            case RoutingOutcome.SendToReview review -> applyReview(ticket, review, now);
            case RoutingOutcome.RequestMoreInformation more -> applyMoreInformation(ticket, more, now);
        }

        // La auditoría va en la misma transacción que el cambio: una decisión aplicada que no
        // quedó registrada no se podría explicar después (RF-09).
        decisionLog.record(DecisionRecord.of(
                ticket.id(), decisions.name(), decided.judgement(), outcome, latencyMs, now));

        return outcome;
    }

    private static Duration elapsedSince(long nanos) {
        return Duration.ofNanos(System.nanoTime() - nanos);
    }

    /** El resultado junto con el juicio que lo produjo, que puede no existir. */
    private record Decided(RoutingOutcome outcome, JudgementSnapshot judgement) {
    }

    private Decided judgeAndDecide(Ticket ticket, Instant now) {
        TicketJudgement judgement;
        long askedAt = System.nanoTime();
        try {
            judgement = decisions.judge(new TicketContext(ticket.title(), ticket.description(), skills.all()));
            metrics.judged(decisions.name(), elapsedSince(askedAt), true);
        } catch (DecisionUnavailableException e) {
            // Se mide igual: una caída del proveedor que tarda diez segundos en fallar es un
            // problema distinto de una que falla al instante, y el promedio de las respuestas
            // buenas no lo muestra.
            metrics.judged(decisions.name(), elapsedSince(askedAt), false);
            // RF-07: la falla no se convierte en un juicio. El ticket queda esperando a una
            // persona, y no se pierde.
            log.warn("Decision provider unavailable, sending the ticket to review ticketId={} reason={}",
                    ticket.id().value(), e.getMessage());
            return new Decided(new RoutingOutcome.SendToReview(
                    ReviewReason.PROVIDER_UNAVAILABLE,
                    "el proveedor de decisiones no respondió: " + e.getMessage(),
                    List.of(),
                    policy.version()), null);
        }

        List<RoutingCandidate> candidates = employees.findBySkill(judgement.suggestedSkill()).stream()
                .map(RouteTicketUseCase::toCandidate)
                .toList();

        return new Decided(
                RoutingRules.decide(judgement, candidates, ticket.timeToSlaDeadline(now), policy),
                JudgementSnapshot.of(judgement));
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

    private void applyMoreInformation(Ticket ticket, RoutingOutcome.RequestMoreInformation more,
                                      Instant now) {
        ticket.requestMoreInformation(more.rationale());
        tickets.save(ticket, null);
        outbox.record(OutboxEvent.of(RoutingEvents.TICKET_NEEDS_MORE_INFORMATION,
                ticket.id().value(),
                """
                {"ticketId":"%s","policyVersion":"%s"}"""
                        .formatted(ticket.id().value(), more.policyVersion()), now));

        log.info("Ticket needs more information ticketId={} policyVersion={}",
                ticket.id().value(), more.policyVersion());
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

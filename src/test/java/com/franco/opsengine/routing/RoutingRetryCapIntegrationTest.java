package com.franco.opsengine.routing;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.employee.application.EmployeeRepository;
import com.franco.opsengine.employee.domain.Clearance;
import com.franco.opsengine.employee.domain.Employee;
import com.franco.opsengine.routing.adapter.scheduler.RoutingWorker;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.support.AbstractIntegrationTest;
import com.franco.opsengine.team.application.TeamService;
import com.franco.opsengine.ticket.application.TicketRepository;
import com.franco.opsengine.ticket.domain.Impact;
import com.franco.opsengine.ticket.domain.Ticket;
import com.franco.opsengine.ticket.domain.TicketId;
import com.franco.opsengine.ticket.domain.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un proveedor que siempre falla con un error inesperado: el worker tiene que dejar de
 * reintentar el ticket al llegar al tope y mandarlo a una persona (ADR-016).
 *
 * <p>El proveedor es un doble escrito a mano y no un mock: el worker lo consulta al armar las
 * métricas, antes de que un mock pueda tener stubbeado su nombre.
 */
@Import(RoutingRetryCapIntegrationTest.FailingProviderConfig.class)
@TestPropertySource(properties = {
        "app.routing.worker.enabled=true",
        "app.routing.worker.interval-ms=3600000",
        "app.routing.worker.max-attempts=3"
})
class RoutingRetryCapIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Skill BILLING = new Skill("BILLING");

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private TeamService teams;

    @Autowired
    private RoutingWorker worker;

    @Autowired
    private AlwaysFailingDecisionProvider decisions;

    @BeforeEach
    void setUp() {
        clearOperationalData();
        decisions.calls.set(0);
        employees.save(Employee.create(EmployeeId.newId(), "Bruno",
                teams.create("Facturación " + UUID.randomUUID()).id(),
                Set.of(BILLING), 10, Clearance.CRITICAL));
    }

    private TicketId aPendingTicket() {
        Ticket ticket = Ticket.open(TicketId.newId(), "Cobros duplicados",
                "Nos llegaron dos facturas por el mismo mes", Impact.HIGH, BILLING,
                NOW, NOW.plus(Duration.ofHours(8)));
        return tickets.save(ticket, null).id();
    }

    private Ticket stored(TicketId id) {
        return tickets.findById(id).orElseThrow();
    }

    @Test
    void aTicketThatKeepsFailingStaysWaitingBelowTheLimit() {
        TicketId ticket = aPendingTicket();

        worker.routePending();
        worker.routePending();

        assertThat(stored(ticket).status()).isEqualTo(TicketStatus.NUEVO);
        assertThat(stored(ticket).routingAttempts()).isEqualTo(2);
    }

    @Test
    void aTicketThatReachesTheLimitGoesToReviewWithTheRoutingReason() {
        TicketId ticket = aPendingTicket();

        for (int pass = 0; pass < 3; pass++) {
            worker.routePending();
        }

        assertThat(stored(ticket).status()).isEqualTo(TicketStatus.REVISION_REQUERIDA);
        assertThat(stored(ticket).reviewReason())
                .hasValueSatisfying(reason -> assertThat(reason).startsWith("ROUTING_FAILED"));
    }

    @Test
    void aTicketInReviewIsNotJudgedAgain() {
        aPendingTicket();

        for (int pass = 0; pass < 4; pass++) {
            worker.routePending();
        }

        assertThat(decisions.calls.get()).isEqualTo(3);
    }

    /** Siempre falla con un error inesperado y cuenta cuántas veces se le pidió un juicio. */
    static class AlwaysFailingDecisionProvider implements DecisionProvider {

        final AtomicInteger calls = new AtomicInteger();

        @Override
        public TicketJudgement judge(TicketContext context) {
            calls.incrementAndGet();
            throw new IllegalStateException("el proveedor explotó");
        }

        @Override
        public String name() {
            return "fake";
        }
    }

    @TestConfiguration
    static class FailingProviderConfig {

        @Bean
        @Primary
        AlwaysFailingDecisionProvider alwaysFailingDecisionProvider() {
            return new AlwaysFailingDecisionProvider();
        }
    }
}

package com.franco.opsengine.routing;

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
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El worker que rutea en segundo plano.
 *
 * <p>Se lo invoca a mano en lugar de esperar al {@code @Scheduled}: un test que espera a un
 * temporizador es lento y se vuelve inestable. Lo que importa es qué hace cuando corre.
 */
@TestPropertySource(properties = {
        "app.routing.worker.enabled=true",
        // Una hora: el temporizador no llega a dispararse durante el test y las pasadas las
        // controla el test. Un worker corriendo de fondo haría los resultados impredecibles.
        "app.routing.worker.interval-ms=3600000"
})
class RoutingWorkerTest extends AbstractIntegrationTest {

    private static final Skill BILLING = new Skill("BILLING");

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private TeamService teams;

    @Autowired
    private RoutingWorker worker;

    @BeforeEach
    void setUp() {
        clearOperationalData();
    }

    private void anAgentWithBillingSkill() {
        employees.save(Employee.create(EmployeeId.newId(), "Bruno",
                teams.create("Facturación " + UUID.randomUUID()).id(),
                Set.of(BILLING), 10, Clearance.CRITICAL));
    }

    private TicketId aPendingTicket(String title, String description) {
        Instant now = Instant.now();
        Ticket ticket = Ticket.open(TicketId.newId(), title, description, Impact.HIGH, BILLING,
                now, now.plus(Duration.ofHours(8)));
        return tickets.save(ticket, null).id();
    }

    private TicketStatus statusOf(TicketId id) {
        return tickets.findById(id).orElseThrow().status();
    }

    @Test
    void routesTicketsThatAreWaiting() {
        anAgentWithBillingSkill();
        TicketId ticket = aPendingTicket("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");

        worker.routePending();

        assertThat(statusOf(ticket)).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void routesTheWholeBatchInOnePass() {
        anAgentWithBillingSkill();
        TicketId first = aPendingTicket("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");
        TicketId second = aPendingTicket("Cobros duplicados", "Otras facturas repetidas en la cuenta");

        worker.routePending();

        assertThat(statusOf(first)).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(statusOf(second)).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void leavesNothingPendingAfterRunning() {
        anAgentWithBillingSkill();
        aPendingTicket("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");

        worker.routePending();

        assertThat(tickets.findByStatus(TicketStatus.NUEVO, 10)).isEmpty();
    }

    @Test
    void doesNotTouchATicketItAlreadyRouted() {
        anAgentWithBillingSkill();
        TicketId ticket = aPendingTicket("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");
        worker.routePending();

        // Una segunda pasada no lo vuelve a tomar: ya no está en NUEVO.
        worker.routePending();

        assertThat(statusOf(ticket)).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(tickets.findById(ticket).orElseThrow().assignee()).isPresent();
    }

    @Test
    void anUnreadableTicketEndsUpAskingTheCustomerForDetails() {
        anAgentWithBillingSkill();
        TicketId ticket = aPendingTicket("asd", "sadnsdns");

        worker.routePending();

        assertThat(statusOf(ticket)).isEqualTo(TicketStatus.INFORMACION_INSUFICIENTE);
    }

    @Test
    void aTicketWithNobodyToTakeItGoesToReview() {
        // Sin agentes cargados no hay a quién asignar.
        TicketId ticket = aPendingTicket("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");

        worker.routePending();

        assertThat(statusOf(ticket)).isEqualTo(TicketStatus.REVISION_REQUERIDA);
    }

    @Test
    void runningWithNothingPendingIsHarmless() {
        worker.routePending();

        assertThat(tickets.findByStatus(TicketStatus.NUEVO, 10)).isEmpty();
    }
}

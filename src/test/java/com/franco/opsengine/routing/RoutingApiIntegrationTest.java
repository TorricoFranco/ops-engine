package com.franco.opsengine.routing;

import com.franco.opsengine.outbox.application.OutboxRepository;
import com.franco.opsengine.routing.domain.ReviewReason;
import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ruteo de punta a punta contra PostgreSQL real, con el proveedor falso.
 */
class RoutingApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepository outbox;

    private RestClient admin;
    private RestClient agent;

    @BeforeEach
    void setUp() {
        clearOperationalData();
        users.create("routing-admin", Role.ADMIN);
        users.create("routing-agent", Role.AGENT);
        admin = clientAs("routing-admin");
        agent = clientAs("routing-agent");
    }

    private UUID createTeam() {
        Map<?, ?> team = admin.post().uri("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Equipo " + UUID.randomUUID()))
                .retrieve().body(Map.class);
        return UUID.fromString((String) team.get("id"));
    }

    private UUID createEmployee(UUID teamId, String name, String skill, int maxOpen, String clearance) {
        Map<?, ?> employee = admin.post().uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", name,
                        "teamId", teamId.toString(),
                        "skills", List.of(skill),
                        "maxOpenTickets", maxOpen,
                        "clearance", clearance))
                .retrieve().body(Map.class);
        return UUID.fromString((String) employee.get("id"));
    }

    private UUID createTicket(String title, String description, int slaMinutes) {
        Map<?, ?> ticket = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "title", title,
                        "description", description,
                        "impact", "HIGH",
                        "requiredSkill", "BILLING",
                        "slaMinutes", slaMinutes))
                .retrieve().body(Map.class);
        return UUID.fromString((String) ticket.get("id"));
    }

    private ResponseEntity<Map> route(UUID ticketId) {
        return agent.post().uri("/api/tickets/" + ticketId + "/routing").retrieve().toEntity(Map.class);
    }

    @Test
    void assignsATicketAndExplainsWhy() {
        UUID team = createTeam();
        UUID bruno = createEmployee(team, "Bruno", "BILLING", 5, "STANDARD");
        UUID ticket = createTicket("Cobros duplicados", "Nos cobraron dos facturas por el mismo mes", 480);

        ResponseEntity<Map> response = route(ticket);

        assertThat(statusOf(response)).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("outcome", "ASSIGNED");
        assertThat(response.getBody()).containsEntry("assigneeId", bruno.toString());
        assertThat(response.getBody().get("policyVersion")).isNotNull();
        assertThat((String) response.getBody().get("rationale")).contains("Bruno");
    }

    @Test
    void theTicketEndsUpAssignedInTheDatabase() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING", 5, "STANDARD");
        UUID ticket = createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 480);

        route(ticket);

        Map stored = agent.get().uri("/api/tickets/" + ticket).retrieve().body(Map.class);
        assertThat(stored).containsEntry("status", "ASIGNADO");
        assertThat(stored.get("assigneeId")).isNotNull();
    }

    @Test
    void theAssignedAgentLoadGoesUp() {
        UUID team = createTeam();
        UUID bruno = createEmployee(team, "Bruno", "BILLING", 5, "STANDARD");
        route(createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 480));

        Map employee = agent.get().uri("/api/employees/" + bruno).retrieve().body(Map.class);

        assertThat(employee).containsEntry("openTickets", 1);
    }

    @Test
    void sendsToReviewWhenNobodyHasTheSkill() {
        UUID team = createTeam();
        createEmployee(team, "Ana", "ACCESS_IDENTITY", 5, "STANDARD");
        UUID ticket = createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 480);

        ResponseEntity<Map> response = route(ticket);

        assertThat(response.getBody()).containsEntry("outcome", "SENT_TO_REVIEW");
        assertThat(response.getBody()).containsEntry("reviewReason", ReviewReason.NO_ELIGIBLE_AGENT.name());

        Map stored = agent.get().uri("/api/tickets/" + ticket).retrieve().body(Map.class);
        assertThat(stored).containsEntry("status", "REVISION_REQUERIDA");
        assertThat(stored.get("reviewReason")).isNotNull();
    }

    @Test
    void listsDiscardedCandidatesWithTheirReason() {
        UUID team = createTeam();
        createEmployee(team, "Lleno", "BILLING", 1, "STANDARD");

        // Lo llenamos con un ticket previo. Se verifica que haya entrado, para que un fallo
        // acá no se confunda con el que busca este test.
        ResponseEntity<Map> first = route(createTicket(
                "Cobros duplicados", "Nos llegaron dos facturas por el mismo mes", 480));
        assertThat(first.getBody()).containsEntry("outcome", "ASSIGNED");

        UUID second = createTicket("Cobros duplicados", "Otras facturas repetidas en la cuenta", 480);
        ResponseEntity<Map> response = route(second);

        assertThat(response.getBody()).containsEntry("outcome", "SENT_TO_REVIEW");
        List<?> candidates = (List<?>) response.getBody().get("candidates");
        assertThat(candidates).hasSize(1);
        Map candidate = (Map) candidates.getFirst();
        assertThat(candidate).containsEntry("eligible", false);
        assertThat(candidate).containsEntry("rejectionReason", "AT_CAPACITY");
    }

    @Test
    void anExpiringSlaRequiresClearance() {
        UUID team = createTeam();
        createEmployee(team, "Sin habilitación", "BILLING", 5, "STANDARD");
        // 10 minutos de SLA: por debajo de la ventana de riesgo, así que el caso es crítico.
        UUID ticket = createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 10);

        ResponseEntity<Map> response = route(ticket);

        assertThat(response.getBody()).containsEntry("outcome", "SENT_TO_REVIEW");
        List<?> candidates = (List<?>) response.getBody().get("candidates");
        assertThat((Map) candidates.getFirst())
                .containsEntry("rejectionReason", "INSUFFICIENT_CLEARANCE");
    }

    @Test
    void anExpiringSlaGoesToTheAgentWithClearance() {
        UUID team = createTeam();
        UUID diego = createEmployee(team, "Diego", "BILLING", 5, "CRITICAL");
        UUID ticket = createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 10);

        ResponseEntity<Map> response = route(ticket);

        assertThat(response.getBody()).containsEntry("outcome", "ASSIGNED");
        assertThat(response.getBody()).containsEntry("assigneeId", diego.toString());
    }

    @Test
    void recordsAnOutboxEventForTheAssignment() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING", 5, "STANDARD");
        long before = outbox.countPending();

        route(createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 480));

        // El despachador está apagado en el perfil de test, así que el evento sigue pendiente.
        assertThat(outbox.countPending()).isGreaterThan(before);
    }

    @Test
    void aTicketCannotBeRoutedTwice() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING", 5, "STANDARD");
        UUID ticket = createTicket("Cobros duplicados", "Dos facturas por el mismo mes", 480);
        route(ticket);

        // Ya está ASIGNADO: la máquina de estados rechaza volver a evaluarlo.
        ResponseEntity<Map> second = route(ticket);

        assertThat(statusOf(second)).isEqualTo(409);
    }

    @Test
    void returns404ForAnUnknownTicket() {
        assertThat(statusOf(route(UUID.randomUUID()))).isEqualTo(404);
    }

    @Test
    void anonymousRequestsAreRejected() {
        ResponseEntity<String> response = client().post()
                .uri("/api/tickets/" + UUID.randomUUID() + "/routing")
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(401);
    }
}

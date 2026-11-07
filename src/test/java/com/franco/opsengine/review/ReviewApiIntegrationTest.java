package com.franco.opsengine.review;

import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bandeja de revisión y auditoría de punta a punta.
 */
class ReviewApiIntegrationTest extends AbstractIntegrationTest {

    private RestClient admin;
    private RestClient agent;
    private RestClient supervisor;

    @BeforeEach
    void setUp() {
        clearOperationalData();
        users.create("review-admin", Role.ADMIN);
        users.create("review-agent", Role.AGENT);
        users.create("review-supervisor", Role.SUPERVISOR);
        admin = clientAs("review-admin");
        agent = clientAs("review-agent");
        supervisor = clientAs("review-supervisor");
    }

    private UUID createTeam() {
        Map<?, ?> team = admin.post().uri("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Equipo " + UUID.randomUUID()))
                .retrieve().body(Map.class);
        return UUID.fromString((String) team.get("id"));
    }

    private UUID createEmployee(UUID teamId, String name, String skill) {
        return createEmployee(teamId, name, skill, 5);
    }

    private UUID createEmployee(UUID teamId, String name, String skill, int maxOpenTickets) {
        Map<?, ?> employee = admin.post().uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "teamId", teamId.toString(),
                        "skills", List.of(skill), "maxOpenTickets", maxOpenTickets,
                        "clearance", "STANDARD"))
                .retrieve().body(Map.class);
        return UUID.fromString((String) employee.get("id"));
    }

    private UUID createTicket() {
        Map<?, ?> ticket = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Cobros duplicados",
                        "description", "Nos llegaron dos facturas por el mismo mes",
                        "impact", "HIGH", "requiredSkill", "BILLING", "slaMinutes", 480))
                .retrieve().body(Map.class);
        return UUID.fromString((String) ticket.get("id"));
    }

    private Map route(UUID ticketId) {
        return agent.post().uri("/api/tickets/" + ticketId + "/routing").retrieve().body(Map.class);
    }

    /**
     * Un ticket que termina en la bandeja porque el único agente con la competencia está lleno.
     *
     * <p>Se lo llena de verdad en lugar de no crear a nadie: el ruteo busca candidatos por
     * competencia, así que sin un agente de BILLING la lista de evaluados vendría vacía y no
     * se podría comprobar que la auditoría guarda los descartes.
     */
    private UUID ticketInReview() {
        UUID team = createTeam();
        createEmployee(team, "Lleno", "BILLING", 1);

        UUID first = createTicket();
        assertThat(route(first)).containsEntry("outcome", "ASSIGNED");

        UUID ticket = createTicket();
        assertThat(route(ticket)).containsEntry("outcome", "SENT_TO_REVIEW");
        return ticket;
    }

    // --- Auditoría ---

    @Test
    void recordsADecisionForEveryRouting() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING");
        UUID ticket = createTicket();
        route(ticket);

        List<?> decisions = agent.get().uri("/api/tickets/" + ticket + "/decisions")
                .retrieve().body(List.class);

        assertThat(decisions).hasSize(1);
        Map decision = (Map) decisions.getFirst();
        assertThat(decision).containsEntry("outcome", "ASSIGNED");
        assertThat(decision).containsEntry("provider", "fake");
        assertThat(decision.get("policyVersion")).isNotNull();
        assertThat(decision.get("latencyMs")).isNotNull();
    }

    @Test
    void theAuditKeepsTheProbabilitiesNotJustTheConclusion() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING");
        UUID ticket = createTicket();
        route(ticket);

        Map decision = (Map) ((List<?>) agent.get().uri("/api/tickets/" + ticket + "/decisions")
                .retrieve().body(List.class)).getFirst();
        Map judgement = (Map) decision.get("judgement");

        assertThat(judgement).containsEntry("suggestedSkill", "BILLING");
        assertThat((Double) judgement.get("skillConfidence")).isBetween(0.0, 1.0);
        assertThat((Double) judgement.get("skillRecognized")).isBetween(0.0, 1.0);
    }

    @Test
    void theAuditKeepsTheDiscardedCandidates() {
        UUID ticket = ticketInReview();

        Map decision = (Map) ((List<?>) agent.get().uri("/api/tickets/" + ticket + "/decisions")
                .retrieve().body(List.class)).getFirst();

        assertThat(decision).containsEntry("outcome", "SENT_TO_REVIEW");
        assertThat(decision).containsEntry("reviewReason", "NO_ELIGIBLE_AGENT");

        List<?> candidates = (List<?>) decision.get("candidates");
        assertThat(candidates).hasSize(1);
        assertThat((Map) candidates.getFirst()).containsEntry("rejectionReason", "AT_CAPACITY");
    }

    @Test
    void theAuditDoesNotCopyTheTicketText() {
        // El texto vive en el ticket. Duplicarlo acá multiplicaría datos personales (RNF-04).
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING");
        UUID ticket = createTicket();
        route(ticket);

        String raw = agent.get().uri("/api/tickets/" + ticket + "/decisions")
                .retrieve().body(String.class);

        assertThat(raw).doesNotContain("Nos llegaron dos facturas por el mismo mes");
    }

    // --- Bandeja ---

    @Test
    void thePendingInboxShowsTheTicketAndWhyItIsThere() {
        UUID ticket = ticketInReview();

        List<?> pending = supervisor.get().uri("/api/review").retrieve().body(List.class);

        assertThat(pending).hasSize(1);
        Map item = (Map) pending.getFirst();
        assertThat(item).containsEntry("ticketId", ticket.toString());
        assertThat(item).containsEntry("status", "REVISION_REQUERIDA");
        assertThat(item).containsEntry("reviewReason", "NO_ELIGIBLE_AGENT");
        assertThat(item.get("rationale")).isNotNull();
    }

    @Test
    void anAssignedTicketIsNotInTheInbox() {
        UUID team = createTeam();
        createEmployee(team, "Bruno", "BILLING");
        route(createTicket());

        assertThat((List<?>) supervisor.get().uri("/api/review").retrieve().body(List.class)).isEmpty();
    }

    // --- Resolución ---

    @Test
    void aSupervisorAssignsFromReviewAndTheTicketMovesOn() {
        UUID ticket = ticketInReview();
        UUID bruno = createEmployee(createTeam(), "Bruno", "BILLING");

        ResponseEntity<Map> response = supervisor.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", bruno.toString(), "agreedWithSuggestion", true))
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("resolution", "ASSIGNED");
        assertThat(response.getBody()).containsEntry("override", false);
        assertThat(response.getBody()).containsEntry("resolvedBy", "review-supervisor");

        Map stored = agent.get().uri("/api/tickets/" + ticket).retrieve().body(Map.class);
        assertThat(stored).containsEntry("status", "ASIGNADO");
    }

    @Test
    void aDisagreementIsRecordedWithItsReason() {
        UUID ticket = ticketInReview();
        UUID bruno = createEmployee(createTeam(), "Bruno", "BILLING");

        Map resolution = supervisor.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", bruno.toString(),
                        "agreedWithSuggestion", false,
                        "disagreementReason", "la competencia sugerida no era la correcta"))
                .retrieve().body(Map.class);

        assertThat(resolution).containsEntry("override", true);
        assertThat(resolution).containsEntry("disagreementReason",
                "la competencia sugerida no era la correcta");
    }

    @Test
    void aDisagreementWithoutAReasonIsRejected() {
        UUID ticket = ticketInReview();
        UUID bruno = createEmployee(createTeam(), "Bruno", "BILLING");

        ResponseEntity<Map> response = supervisor.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", bruno.toString(), "agreedWithSuggestion", false))
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(400);
    }

    @Test
    void aSupervisorCanCloseATicketWithoutAssigningIt() {
        UUID ticket = ticketInReview();

        Map resolution = supervisor.post().uri("/api/review/" + ticket + "/close")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("agreedWithSuggestion", true))
                .retrieve().body(Map.class);

        assertThat(resolution).containsEntry("resolution", "CLOSED");
        Map stored = agent.get().uri("/api/tickets/" + ticket).retrieve().body(Map.class);
        assertThat(stored).containsEntry("status", "CERRADO");
    }

    @Test
    void anAgentCannotResolveAReview() {
        UUID ticket = ticketInReview();
        UUID bruno = createEmployee(createTeam(), "Bruno", "BILLING");

        ResponseEntity<String> response = agent.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", bruno.toString(), "agreedWithSuggestion", true))
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(403);
    }

    @Test
    void theAssignedAgentMustBeAbleToTakeTheTicket() {
        UUID ticket = ticketInReview();
        UUID ana = createEmployee(createTeam(), "Ana sin la skill", "ACCESS_IDENTITY");

        ResponseEntity<Map> response = supervisor.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", ana.toString(), "agreedWithSuggestion", true))
                .retrieve().toEntity(Map.class);

        // La persona elige, pero las restricciones del dominio siguen valiendo.
        assertThat(statusOf(response)).isEqualTo(409);
    }

    // --- Métricas ---

    @Test
    void theMetricsCountReviewsAndOverrides() {
        UUID ticket = ticketInReview();
        UUID bruno = createEmployee(createTeam(), "Bruno", "BILLING");
        supervisor.post().uri("/api/review/" + ticket + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("employeeId", bruno.toString(),
                        "agreedWithSuggestion", false,
                        "disagreementReason", "era de facturación"))
                .retrieve().toBodilessEntity();

        Map metrics = supervisor.get().uri("/api/review/metrics").retrieve().body(Map.class);

        assertThat(metrics).containsEntry("sentToReview", 1);
        assertThat(metrics).containsEntry("resolutions", 1);
        assertThat(metrics).containsEntry("comparable", 1);
        assertThat(metrics).containsEntry("overrides", 1);
        assertThat(metrics).containsEntry("overrideRate", 1.0);
    }

    @Test
    void theOverrideRateIsEmptyWhenThereIsNothingToCompare() {
        // Un cero inventado se leería como que el motor nunca se equivoca.
        Map metrics = supervisor.get().uri("/api/review/metrics").retrieve().body(Map.class);

        assertThat(metrics).containsEntry("comparable", 0);
        assertThat(metrics.get("overrideRate")).isNull();
    }
}

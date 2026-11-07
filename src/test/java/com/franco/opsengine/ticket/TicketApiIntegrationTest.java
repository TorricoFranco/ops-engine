package com.franco.opsengine.ticket;

import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketApiIntegrationTest extends AbstractIntegrationTest {

    private static final Map<String, Object> VALID_TICKET = Map.of(
            "title", "Cobros duplicados",
            "description", "Desde la última actualización hay clientes con cobros duplicados",
            "impact", "HIGH",
            "requiredSkill", "BILLING",
            "slaMinutes", 240);

    private RestClient agent;

    @BeforeEach
    void setUp() {
        users.create("ticket-agent", Role.AGENT);
        agent = clientAs("ticket-agent");
    }

    @Test
    void createsATicketInNuevo() {
        ResponseEntity<Map> response = agent.post()
                .uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(VALID_TICKET)
                .retrieve()
                .toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("status", "NUEVO");
        assertThat(response.getBody()).containsEntry("requiredSkill", "BILLING");
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void anonymousRequestsAreRejected() {
        ResponseEntity<String> response = client().get()
                .uri("/api/tickets")
                .retrieve()
                .toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(401);
    }

    @Test
    void storesAndReadsBackTheTicket() {
        Map created = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).body(VALID_TICKET)
                .retrieve().body(Map.class);

        Map found = agent.get().uri("/api/tickets/" + created.get("id"))
                .retrieve().body(Map.class);

        assertThat(found).containsEntry("id", created.get("id"));
        assertThat(found).containsEntry("title", "Cobros duplicados");
    }

    @Test
    void sameIdempotencyKeyReturnsTheSameTicket() {
        String key = "key-" + UUID.randomUUID();

        Map first = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .body(VALID_TICKET)
                .retrieve().body(Map.class);

        Map second = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .body(VALID_TICKET)
                .retrieve().body(Map.class);

        assertThat(second.get("id")).isEqualTo(first.get("id"));
    }

    @Test
    void differentIdempotencyKeysCreateDifferentTickets() {
        Map first = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "key-" + UUID.randomUUID())
                .body(VALID_TICKET)
                .retrieve().body(Map.class);

        Map second = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "key-" + UUID.randomUUID())
                .body(VALID_TICKET)
                .retrieve().body(Map.class);

        assertThat(second.get("id")).isNotEqualTo(first.get("id"));
    }

    @Test
    void requestsWithoutAKeyAreNotDeduplicated() {
        Map first = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).body(VALID_TICKET)
                .retrieve().body(Map.class);

        Map second = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).body(VALID_TICKET)
                .retrieve().body(Map.class);

        assertThat(second.get("id")).isNotEqualTo(first.get("id"));
    }

    @Test
    void rejectsAnInvalidPayloadWith400() {
        Map<String, Object> invalid = Map.of(
                "title", "",
                "description", "sin título",
                "impact", "HIGH",
                "requiredSkill", "BILLING",
                "slaMinutes", 10);

        ResponseEntity<Map> response = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).body(invalid)
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
    }

    @Test
    void rejectsANonPositiveSlaWith400() {
        Map<String, Object> invalid = Map.of(
                "title", "Título",
                "description", "descripción",
                "impact", "LOW",
                "requiredSkill", "BILLING",
                "slaMinutes", 0);

        ResponseEntity<Map> response = agent.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).body(invalid)
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(400);
    }

    @Test
    void returns404ForAnUnknownTicket() {
        ResponseEntity<Map> response = agent.get()
                .uri("/api/tickets/" + UUID.randomUUID())
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(response)).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
    }
}

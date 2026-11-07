package com.franco.opsengine.security;

import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        users.create("auth-agent", Role.AGENT);
        users.create("auth-admin", Role.ADMIN);
    }

    @Test
    void loginReturnsAUsableToken() {
        ResponseEntity<Map> login = client().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "auth-agent", "password", TestUserFactory.PASSWORD))
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(login)).isEqualTo(200);
        assertThat(login.getBody()).containsEntry("tokenType", "Bearer");
        assertThat(login.getBody()).containsEntry("role", "AGENT");

        String token = (String) login.getBody().get("accessToken");
        ResponseEntity<String> tickets = client().get()
                .uri("/api/tickets")
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(String.class);

        assertThat(statusOf(tickets)).isEqualTo(200);
    }

    @Test
    void rejectsAWrongPassword() {
        ResponseEntity<String> response = client().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "auth-agent", "password", "not-the-password"))
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(401);
    }

    @Test
    void rejectsAnUnknownUser() {
        ResponseEntity<String> response = client().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "does-not-exist", "password", TestUserFactory.PASSWORD))
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(401);
    }

    @Test
    void rejectsAGarbageToken() {
        ResponseEntity<String> response = client().get()
                .uri("/api/tickets")
                .header("Authorization", "Bearer not-a-jwt")
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(401);
    }

    @Test
    void anAgentCannotCreateTeams() {
        ResponseEntity<String> response = clientAs("auth-agent").post()
                .uri("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Equipo de prueba " + UUID.randomUUID()))
                .retrieve().toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(403);
    }

    @Test
    void anAdminCanCreateTeamsAndEmployees() {
        ResponseEntity<Map> team = clientAs("auth-admin").post()
                .uri("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Facturación " + UUID.randomUUID()))
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(team)).isEqualTo(201);

        ResponseEntity<Map> employee = clientAs("auth-admin").post()
                .uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Bruno Giménez",
                        "teamId", team.getBody().get("id"),
                        "skills", java.util.List.of("billing"),
                        "maxOpenTickets", 5))
                .retrieve().toEntity(Map.class);

        assertThat(statusOf(employee)).isEqualTo(201);
        // La skill se normaliza a mayúsculas en el dominio.
        assertThat(employee.getBody().get("skills")).isEqualTo(java.util.List.of("BILLING"));
        assertThat(employee.getBody()).containsEntry("availability", "AVAILABLE");
        assertThat(employee.getBody()).containsEntry("openTickets", 0);
    }
}

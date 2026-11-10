package com.franco.opsengine.api;

import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ClientErrorsApiIntegrationTest extends AbstractIntegrationTest {

    private RestClient agent;

    @BeforeEach
    void setUp() {
        users.create("client-errors-agent", Role.AGENT);
        agent = clientAs("client-errors-agent");
    }

    @Test
    void malformedJsonBodyIsRejectedAsBadRequest() {
        ResponseEntity<String> response = agent.post()
                .uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\": ")
                .retrieve()
                .toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(400);
        assertThat(response.getBody()).contains("JSON válido");
    }

    @Test
    void malformedPathVariableIsRejectedAsBadRequest() {
        ResponseEntity<String> response = agent.get()
                .uri("/api/tickets/no-es-un-uuid")
                .retrieve()
                .toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(400);
        assertThat(response.getBody()).contains("tiene un formato inválido");
    }

    @Test
    void unsupportedHttpMethodIsRejectedAsMethodNotAllowed() {
        ResponseEntity<String> response = agent.delete()
                .uri("/api/tickets")
                .retrieve()
                .toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(405);
    }

    @Test
    void unsupportedContentTypeIsRejectedAsUnsupportedMediaType() {
        ResponseEntity<String> response = agent.post()
                .uri("/api/tickets")
                .contentType(MediaType.TEXT_PLAIN)
                .body("hola")
                .retrieve()
                .toEntity(String.class);

        assertThat(statusOf(response)).isEqualTo(415);
    }
}

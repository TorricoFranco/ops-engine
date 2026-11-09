package com.franco.opsengine;

import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndpointTest extends AbstractIntegrationTest {

    @Test
    void healthEndpointIsPublicAndReportsUp() {
        String body = managementClient().get().uri("/actuator/health").retrieve().body(String.class);

        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorIsNotServedOnTheApiPort() {
        // El puerto de la API es el que se publica al exterior. Que no responda acá es lo
        // que permite dejar las métricas sin autenticación en el puerto de monitoreo.
        ResponseEntity<String> response =
                client().get().uri("/actuator/health").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isIn(401, 404);
    }
}

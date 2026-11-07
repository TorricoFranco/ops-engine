package com.franco.opsengine;

import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndpointTest extends AbstractIntegrationTest {

    @Test
    void healthEndpointIsPublicAndReportsUp() {
        String body = client().get().uri("/actuator/health").retrieve().body(String.class);

        assertThat(body).contains("\"status\":\"UP\"");
    }
}

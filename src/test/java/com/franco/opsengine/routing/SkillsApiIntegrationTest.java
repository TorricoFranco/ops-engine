package com.franco.opsengine.routing;

import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsApiIntegrationTest extends AbstractIntegrationTest {

    private RestClient agent;

    @BeforeEach
    void setUp() {
        users.create("skills-agent", Role.AGENT);
        agent = clientAs("skills-agent");
    }

    @Test
    void listsEverySupportAreaWithTheNameItIsShownUnder() {
        List<Map<String, String>> skills = agent.get()
                .uri("/api/skills")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, String>>>() {});

        Map<String, String> names = skills.stream()
                .collect(Collectors.toMap(skill -> skill.get("code"), skill -> skill.get("name")));

        assertThat(names)
                .hasSize(5)
                .containsEntry("ACCESS_IDENTITY", "Accesos e identidad")
                .containsEntry("BILLING", "Facturación")
                .containsEntry("INTEGRATIONS", "Integraciones")
                .containsEntry("INFRASTRUCTURE", "Infraestructura")
                .containsEntry("ACCOUNT_MANAGEMENT", "Gestión de cuentas");
    }
}

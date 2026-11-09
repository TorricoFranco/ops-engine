package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.shared.domain.Skill;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Piezas compartidas por los tests del adapter de Jev. */
final class JevTestSupport {

    static final Map<Skill, String> SKILLS = Map.of(
            new Skill("BILLING"), "Cobros, facturas y reembolsos.",
            new Skill("ACCESS_IDENTITY"), "Inicio de sesión y permisos.");

    private JevTestSupport() {
    }

    static TicketContext context() {
        return new TicketContext("Cobros duplicados", "Nos cobraron dos veces este mes", SKILLS);
    }

    /** Holgado a propósito: el primer pedido contra WireMock puede tardar en arrancar. */
    static JevProperties properties(String baseUrl, int maxAttempts) {
        return properties(baseUrl, maxAttempts, Duration.ofSeconds(5));
    }

    static JevProperties properties(String baseUrl, int maxAttempts, Duration readTimeout) {
        return new JevProperties(baseUrl, "test-key", "jev-latest",
                Duration.ofSeconds(2), readTimeout, maxAttempts);
    }

    /**
     * Igual que el cliente de producción salvo por el protocolo: se fija HTTP/1.1 porque el
     * intento de upgrade a HTTP/2 del cliente JDK hace que WireMock corte la conexión.
     */
    static RestClient restClient(JevProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Circuito que nunca se abre: aísla el comportamiento que cada test quiere observar. */
    static CircuitBreaker openCircuitNever() {
        return CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(Integer.MAX_VALUE)
                .build());
    }

    static Retry noRetry() {
        return Retry.of("test", RetryConfig.custom().maxAttempts(1).build());
    }

    static JevDecisionProvider provider(String baseUrl) {
        return provider(properties(baseUrl, 1));
    }

    static JevDecisionProvider provider(JevProperties properties) {
        return new JevDecisionProvider(restClient(properties), properties, openCircuitNever(), noRetry());
    }

    /** Respuesta completa y válida, con la forma que documenta TypeSafe. */
    static String validResponse() {
        return """
                {
                  "model": "jev-latest",
                  "answers": {
                    "required_skill": {
                      "type": "choice",
                      "choice": "BILLING",
                      "confidence": 0.82,
                      "probabilities": {"BILLING": 0.86, "ACCESS_IDENTITY": 0.14}
                    },
                    "skill_recognized": {"type": "noul", "noul": 0.94},
                    "urgency": {
                      "type": "score",
                      "score": 2.4,
                      "probabilities": {"0": 0.05, "1": 0.15, "2": 0.3, "3": 0.4, "4": 0.1}
                    },
                    "multiple_customers_affected": {"type": "noul", "noul": 0.71},
                    "service_unavailable": {"type": "noul", "noul": 0.08},
                    "enough_information": {"type": "noul", "noul": 0.93}
                  },
                  "usage": {"input_tokens": 180, "output_tokens": 24}
                }
                """;
    }
}

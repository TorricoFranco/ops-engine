package com.franco.opsengine.decision.adapter.jev;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comportamiento de reintentos y corte de circuito. Lo que importa para el negocio es que
 * ninguna de estas situaciones produzca un juicio inventado: todas terminan en
 * DecisionUnavailableException, y el ruteo manda el ticket a revisión (RF-07).
 */
class JevResilienceTest {

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + server.port();
    }

    private void alwaysFail() {
        server.stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(aResponse().withStatus(503)));
    }

    private JevDecisionProvider providerWith(Retry retry, CircuitBreaker breaker, int maxAttempts) {
        JevProperties properties = JevTestSupport.properties(baseUrl(), maxAttempts);
        return new JevDecisionProvider(
                JevTestSupport.restClient(properties), properties, breaker, retry);
    }

    @Test
    void retriesTransportFailuresUpToTheConfiguredAttempts() {
        alwaysFail();
        Retry retry = Retry.of("test", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .retryOnException(e -> e instanceof DecisionUnavailableException
                        && e.getCause() instanceof RestClientException)
                .build());

        assertThatThrownBy(() -> providerWith(retry, JevTestSupport.openCircuitNever(), 3)
                .judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class);

        server.verify(3, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    @Test
    void doesNotRetryAResponseThatCameBackButCannotBeUnderstood() {
        // Reintentar no cambia una respuesta mal formada: solo gasta tiempo y presupuesto.
        server.stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"model\":\"jev-latest\",\"answers\":{}}")));

        Retry retry = Retry.of("test", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .retryOnException(e -> e instanceof DecisionUnavailableException
                        && e.getCause() instanceof RestClientException)
                .build());

        assertThatThrownBy(() -> providerWith(retry, JevTestSupport.openCircuitNever(), 3)
                .judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class);

        server.verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    @Test
    void stopsCallingTheProviderOnceTheCircuitOpens() {
        alwaysFail();
        CircuitBreaker breaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .recordExceptions(DecisionUnavailableException.class)
                .build());

        JevDecisionProvider provider = providerWith(JevTestSupport.noRetry(), breaker, 1);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                    .isInstanceOf(DecisionUnavailableException.class);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Con el circuito abierto la llamada ni se intenta: el ticket va a revisión enseguida
        // en lugar de esperar otro timeout.
        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class)
                .hasMessageContaining("circuito");

        server.verify(2, postRequestedFor(urlEqualTo("/v1/systemone")));
    }
}

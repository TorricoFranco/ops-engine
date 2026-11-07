package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(JevProperties.class)
@ConditionalOnProperty(name = "app.decision.provider", havingValue = JevDecisionProvider.NAME)
class JevConfig {

    /**
     * Timeouts explícitos: sin ellos una llamada colgada bloquearía el hilo indefinidamente y
     * el circuit breaker nunca llegaría a abrirse.
     */
    @Bean
    RestClient jevRestClient(RestClient.Builder builder, JevProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Abre el circuito cuando más de la mitad de las últimas llamadas falla, y lo deja medio
     * abierto al minuto para probar de nuevo. Mientras está abierto, las llamadas ni se
     * intentan: el ticket va a revisión enseguida en lugar de esperar un timeout.
     */
    @Bean
    CircuitBreaker jevCircuitBreaker() {
        return CircuitBreaker.of("jev", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(DecisionUnavailableException.class)
                .build());
    }

    /**
     * Solo reintenta fallas de transporte. Una respuesta que el proveedor dio pero que no se
     * entiende no mejora reintentando: ese caso va directo a revisión.
     */
    @Bean
    Retry jevRetry(JevProperties properties) {
        return Retry.of("jev", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .waitDuration(Duration.ofMillis(200))
                .retryOnException(JevConfig::isTransport)
                .build());
    }

    private static boolean isTransport(Throwable throwable) {
        return throwable instanceof DecisionUnavailableException
                && throwable.getCause() instanceof org.springframework.web.client.RestClientException;
    }

    @Bean
    DecisionProvider jevDecisionProvider(RestClient jevRestClient, JevProperties properties,
                                         CircuitBreaker jevCircuitBreaker, Retry jevRetry) {
        return new JevDecisionProvider(jevRestClient, properties, jevCircuitBreaker, jevRetry);
    }
}

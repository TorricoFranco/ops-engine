package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Arma un proveedor de Jev sin Spring.
 *
 * <p>Existe para que la configuración del cliente y de la resiliencia esté en un solo lugar:
 * la usa tanto el contexto de la aplicación como la evaluación offline, que corre sin Spring.
 */
public final class JevProviderFactory {

    private JevProviderFactory() {
    }

    public static DecisionProvider create(JevProperties properties) {
        return create(properties, circuitBreaker());
    }

    /**
     * Variante para cuando quien llama necesita el circuit breaker, por ejemplo para
     * publicar su estado como métrica. La evaluación offline usa la otra.
     */
    public static DecisionProvider create(JevProperties properties, CircuitBreaker circuitBreaker) {
        return new JevDecisionProvider(
                restClient(properties), properties, circuitBreaker, retry(properties));
    }

    /**
     * Timeouts explícitos: sin ellos una llamada colgada bloquearía el hilo indefinidamente y
     * el circuit breaker nunca llegaría a abrirse.
     */
    public static RestClient restClient(JevProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
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
    public static CircuitBreaker circuitBreaker() {
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
    public static Retry retry(JevProperties properties) {
        return Retry.of("jev", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .waitDuration(Duration.ofMillis(200))
                .retryOnException(JevProviderFactory::isTransport)
                .build());
    }

    private static boolean isTransport(Throwable throwable) {
        return throwable instanceof DecisionUnavailableException
                && throwable.getCause() instanceof RestClientException;
    }
}

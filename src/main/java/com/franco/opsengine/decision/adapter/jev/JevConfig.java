package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el proveedor de Jev cuando se lo pide con app.decision.provider=jev.
 *
 * <p>La construcción vive en {@link JevProviderFactory}, que también usa la evaluación
 * offline: así el cliente y la resiliencia se configuran en un solo lugar.
 */
@Configuration
@EnableConfigurationProperties(JevProperties.class)
@ConditionalOnProperty(name = "app.decision.provider", havingValue = JevDecisionProvider.NAME)
class JevConfig {

    @Bean
    CircuitBreaker jevCircuitBreaker() {
        return JevProviderFactory.circuitBreaker();
    }

    @Bean
    DecisionProvider jevDecisionProvider(JevProperties properties, CircuitBreaker jevCircuitBreaker) {
        return JevProviderFactory.create(properties, jevCircuitBreaker);
    }

    /**
     * Una serie por estado, en 0 o 1, en lugar de un número que codifique el estado.
     *
     * <p>Un único gauge con valores 0, 1, 2 parece más corto, pero después no se puede
     * alertar sin recordar qué número era "abierto", y un promedio entre dos instancias daría
     * un estado que no existe.
     */
    @Bean
    MeterBinder jevCircuitBreakerMetrics(CircuitBreaker jevCircuitBreaker) {
        return registry -> {
            for (CircuitBreaker.State state : CircuitBreaker.State.values()) {
                Gauge.builder("ops.decision.circuit.state", jevCircuitBreaker,
                                breaker -> breaker.getState() == state ? 1 : 0)
                        .description("Estado del circuit breaker hacia el proveedor de juicios")
                        .tag("state", state.name().toLowerCase())
                        .register(registry);
            }
        };
    }
}

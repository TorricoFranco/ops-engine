package com.franco.opsengine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
class MetricsConfig {

    /**
     * Etiqueta común a toda métrica que salga de acá.
     *
     * <p>Una sola instancia no la necesita, pero en cuanto hay dos entornos apuntando al mismo
     * Prometheus las series se mezclan y los paneles suman cosas que no van juntas.
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name}") String application,
            @Value("${app.environment:local}") String environment) {
        return registry -> registry.config().commonTags("application", application, "environment", environment);
    }

    /**
     * Percentiles de la latencia HTTP calculados del lado del servidor.
     *
     * <p>Micrometer publica solo un contador y una suma por defecto, y con eso no se puede
     * sacar un p95: el promedio esconde justo la cola que importa.
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> httpLatencyPercentiles() {
        return registry -> registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (!id.getName().startsWith("http.server.requests")
                        && !id.getName().startsWith("ops.")) {
                    return config;
                }
                return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .minimumExpectedValue(Duration.ofMillis(5).toNanos())
                        .maximumExpectedValue(Duration.ofSeconds(20).toNanos())
                        .build()
                        .merge(config);
            }
        });
    }
}

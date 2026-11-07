package com.franco.opsengine.routing.adapter.config;

import com.franco.opsengine.routing.domain.RoutingPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Umbrales de ruteo desde configuración.
 *
 * <p>La versión se escribe a mano y se cambia al tocar cualquier umbral: es lo que después
 * permite reconstruir con qué reglas se decidió un ticket viejo. Cuando las reglas tengan que
 * ser editables sin desplegar, esto pasa a una tabla con vigencia desde y hasta.
 */
@Configuration
@EnableConfigurationProperties(RoutingPolicyProperties.Values.class)
class RoutingPolicyProperties {

    @ConfigurationProperties(prefix = "app.routing.policy")
    record Values(
            String version,
            double minSkillRecognized,
            double minSkillConfidence,
            double criticalNoul,
            double criticalUrgency,
            Duration slaAtRiskWindow) {
    }

    @Bean
    RoutingPolicy routingPolicy(Values values) {
        return new RoutingPolicy(
                values.version(),
                values.minSkillRecognized(),
                values.minSkillConfidence(),
                values.criticalNoul(),
                values.criticalUrgency(),
                values.slaAtRiskWindow());
    }
}

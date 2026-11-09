package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
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
    DecisionProvider jevDecisionProvider(JevProperties properties) {
        return JevProviderFactory.create(properties);
    }
}

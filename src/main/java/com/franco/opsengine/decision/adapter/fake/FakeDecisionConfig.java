package com.franco.opsengine.decision.adapter.fake;

import com.franco.opsengine.decision.application.DecisionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proveedor por defecto: sin configuración, el sistema arranca sin llamar a ninguna API
 * externa. Para usar Jev hay que pedirlo con app.decision.provider=jev.
 */
@Configuration
@ConditionalOnProperty(name = "app.decision.provider", havingValue = FakeDecisionProvider.NAME,
        matchIfMissing = true)
class FakeDecisionConfig {

    @Bean
    DecisionProvider fakeDecisionProvider() {
        return new FakeDecisionProvider();
    }
}

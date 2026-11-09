package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La única llamada a la API real de Jev.
 *
 * <p>No corre en CI: surefire excluye el grupo {@code live}. Además se saltea solo si no hay
 * clave, así nadie la dispara sin querer.
 *
 * <p>Para correrla:
 * {@snippet lang = shell : mvn test -Dtest=JevLiveSmokeTest -Dgroups=live}
 *
 * <p>Manda dos tickets de ejemplo inventados. No hay datos de nadie, y el gasto es de
 * centésimas de centavo.
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "TYPESAFE_API_KEY", matches = ".+")
class JevLiveSmokeTest {

    private static final Map<Skill, String> CATALOG = new LinkedHashMap<>();

    static {
        CATALOG.put(new Skill("ACCESS_IDENTITY"),
                "Inicio de sesión, contraseñas, permisos y accesos de usuarios.");
        CATALOG.put(new Skill("BILLING"),
                "Cobros, facturas, reembolsos y problemas con los pagos.");
        CATALOG.put(new Skill("INTEGRATIONS"),
                "Sincronización, webhooks y conexiones con sistemas externos.");
        CATALOG.put(new Skill("INFRASTRUCTURE"),
                "Caídas del servicio, lentitud, errores del servidor y capacidad.");
    }

    private static DecisionProvider provider() {
        return JevProviderFactory.create(new JevProperties(
                "https://api.typesafe.ai",
                System.getenv("TYPESAFE_API_KEY"),
                "jev-latest",
                Duration.ofSeconds(5),
                Duration.ofSeconds(30),
                3));
    }

    @Test
    void answersABillingTicketWithAUsableJudgement() {
        TicketJudgement judgement = provider().judge(new TicketContext(
                "Cobros duplicados",
                "Nos cobraron dos veces la factura de este mes y necesitamos el reintegro",
                CATALOG));

        // Se afirma la forma de la respuesta y que la competencia obvia gane, no un número
        // exacto: fijar una probabilidad concreta rompería con la próxima versión del modelo.
        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("BILLING"));
        assertThat(judgement.skillRecognized().isYes(NoulAnswer.DEFAULT_YES)).isTrue();
        assertThat(judgement.requiredSkill().probabilities()).hasSize(CATALOG.size());
        assertThat(judgement.urgency().levels()).isEqualTo(5);
    }

    @Test
    void recognisesThatAnUnrelatedTicketMatchesNoSkill() {
        TicketJudgement judgement = provider().judge(new TicketContext(
                "Consulta de horarios",
                "Quería saber hasta qué hora atienden los sábados",
                CATALOG));

        assertThat(judgement.skillRecognized().isYes(NoulAnswer.DEFAULT_YES)).isFalse();
    }
}

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
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La única llamada a la API real de Jev.
 *
 * <p>No corre en CI: surefire excluye el grupo {@code live}. Además se saltea solo si no hay
 * clave, así nadie la dispara sin querer. Para correrla:
 *
 * <pre>mvn test -Dtest=JevLiveSmokeTest -Dexcluded.test.groups=</pre>
 *
 * <p>Manda dos tickets de ejemplo inventados. No hay datos de nadie, y el gasto es de
 * centésimas de centavo.
 *
 * <p>Imprime los juicios completos: en un smoke test contra la API real, ver los números es
 * justamente el punto, y son los que después permiten calibrar los umbrales con datos en
 * lugar de con los valores que sugiere la documentación.
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
        CATALOG.put(new Skill("ACCOUNT_MANAGEMENT"),
                "Planes, precios, contratación, cambios de medio de pago y datos de facturación.");
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

    private static TicketJudgement judgeAndReport(String label, String title, String description) {
        TicketJudgement judgement = provider().judge(new TicketContext(title, description, CATALOG));

        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "%n[%s] %s%n", label, title));
        out.append(String.format(Locale.ROOT, "  competencia elegida : %s (p=%.3f, confianza=%.3f)%n",
                judgement.requiredSkill().value(),
                judgement.requiredSkill().probabilityOf(judgement.requiredSkill().value()),
                judgement.requiredSkill().confidence()));
        out.append(String.format(Locale.ROOT, "  corresponde a alguna: %.3f%n",
                judgement.skillRecognized().probability()));
        out.append(String.format(Locale.ROOT, "  urgencia            : %.2f de %d%n",
                judgement.urgency().value(), judgement.urgency().levels() - 1));
        out.append(String.format(Locale.ROOT, "  varios afectados    : %.3f%n",
                judgement.multipleCustomersAffected().probability()));
        out.append(String.format(Locale.ROOT, "  servicio caido      : %.3f%n",
                judgement.serviceUnavailable().probability()));
        out.append("  todas las opciones  : ").append(judgement.requiredSkill().probabilities());
        System.out.println(out);

        return judgement;
    }

    @Test
    void answersABillingTicketWithAUsableJudgement() {
        TicketJudgement judgement = judgeAndReport("facturación",
                "Cobros duplicados",
                "Nos cobraron dos veces la factura de este mes y necesitamos el reintegro");

        // Se afirma la forma de la respuesta y que la competencia obvia gane, no un número
        // exacto: fijar una probabilidad concreta rompería con la próxima versión del modelo.
        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("BILLING"));
        assertThat(judgement.skillRecognized().probability())
                .as("corresponde a alguna competencia")
                .isGreaterThanOrEqualTo(NoulAnswer.DEFAULT_YES);
        assertThat(judgement.requiredSkill().probabilities()).hasSize(CATALOG.size());
        assertThat(judgement.urgency().levels()).isEqualTo(5);
    }

    @Test
    void reportsAWeakerSignalOnAnUnrelatedTicket() {
        TicketJudgement clear = judgeAndReport("facturación",
                "Cobros duplicados",
                "Nos cobraron dos veces la factura de este mes y necesitamos el reintegro");
        TicketJudgement unrelated = judgeAndReport("fuera de alcance",
                "Consulta de horarios",
                "Quería saber hasta qué hora atienden los sábados");

        // No se fija un umbral: se compara contra un caso claro. Lo que tiene que valer es
        // que la señal sea más débil, no que caiga por debajo de un número elegido a dedo.
        assertThat(unrelated.skillRecognized().probability())
                .as("un ticket de otra área tiene que dar una señal más débil que uno claro")
                .isLessThan(clear.skillRecognized().probability());
    }

    @Test
    void aCommercialEnquiryGoesToAccountManagement() {
        // Antes de que existiera el área comercial, este caso servía para comprobar que el
        // sistema sabía derivar lo que no le correspondía. Ahora sí le corresponde, así que
        // lo que se verifica cambió: tiene que asignarse, no derivarse.
        TicketJudgement judgement = judgeAndReport("comercial",
                "Quiero contratar un plan más grande",
                "Necesito saber cómo paso al plan empresarial y qué incluye");

        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("ACCOUNT_MANAGEMENT"));
        assertThat(judgement.skillRecognized().probability())
                .as("una consulta comercial ya es un caso que el equipo atiende")
                .isGreaterThanOrEqualTo(NoulAnswer.DEFAULT_YES);
    }
}

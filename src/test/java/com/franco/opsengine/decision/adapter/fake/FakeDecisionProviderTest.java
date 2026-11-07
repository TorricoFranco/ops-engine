package com.franco.opsengine.decision.adapter.fake;

import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FakeDecisionProviderTest {

    private static final Map<Skill, String> SKILLS = Map.of(
            new Skill("BILLING"), "Cobros duplicados, facturas y reembolsos.",
            new Skill("ACCESS_IDENTITY"), "Inicio de sesión, contraseñas y permisos.",
            new Skill("INTEGRATIONS"), "Sincronización y webhooks con sistemas externos.");

    private final FakeDecisionProvider provider = new FakeDecisionProvider();

    private TicketJudgement judge(String title, String description) {
        return provider.judge(new TicketContext(title, description, SKILLS));
    }

    @Test
    void picksTheSkillWhoseDescriptionMatchesTheTicket() {
        TicketJudgement judgement = judge("Cobros duplicados", "Nos llegaron dos facturas por el mismo mes");

        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("BILLING"));
        assertThat(judgement.skillRecognized().isYes(NoulAnswer.DEFAULT_YES)).isTrue();
    }

    @Test
    void matchesIgnoringAccentsAndCase() {
        TicketJudgement judgement = judge("Problema", "No puedo iniciar SESIÓN con mi contraseña");

        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("ACCESS_IDENTITY"));
    }

    @Test
    void reportsALowSkillRecognizedWhenNothingMatches() {
        TicketJudgement judgement = judge("Consulta", "Quisiera saber el horario de atención");

        assertThat(judgement.skillRecognized().isNo(NoulAnswer.DEFAULT_NO)).isTrue();
    }

    @Test
    void stillReturnsAChoiceEvenWhenNothingMatches() {
        // Un choice siempre elige. Por eso el ruteo mira skillRecognized y no solo la opción.
        TicketJudgement judgement = judge("Consulta", "Quisiera saber el horario de atención");

        assertThat(judgement.requiredSkill().value()).isNotNull();
        assertThat(judgement.requiredSkill().probabilities()).hasSize(3);
    }

    @Test
    void theProbabilitiesOfAChoiceSumToOne() {
        TicketJudgement judgement = judge("Cobros duplicados", "Dos facturas por el mismo mes");

        double total = judgement.requiredSkill().probabilities().values().stream()
                .mapToDouble(Double::doubleValue).sum();

        assertThat(total).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void readsUrgencyFromTheWordingAndTheOutage() {
        TicketJudgement calm = judge("Consulta", "Cuando puedan, me gustaría una factura");
        TicketJudgement urgent = judge("Urgente", "El servicio está caído y es urgente, estamos bloqueados");

        assertThat(urgent.urgency().value()).isGreaterThan(calm.urgency().value());
        assertThat(urgent.serviceUnavailable().isYes(NoulAnswer.DEFAULT_YES)).isTrue();
        assertThat(calm.serviceUnavailable().isNo(NoulAnswer.DEFAULT_NO)).isTrue();
    }

    @Test
    void detectsThatSeveralCustomersAreAffected()  {
        TicketJudgement judgement = judge("Cobros", "Hay varios clientes afectados por el mismo problema");

        assertThat(judgement.multipleCustomersAffected().isYes(NoulAnswer.DEFAULT_YES)).isTrue();
    }

    @Test
    void isDeterministicSoTestsAndDemosDoNotDrift() {
        TicketJudgement first = judge("Cobros duplicados", "Dos facturas por el mismo mes");
        TicketJudgement second = judge("Cobros duplicados", "Dos facturas por el mismo mes");

        assertThat(second).isEqualTo(first);
    }

    @Test
    void identifiesItselfForTheDecisionLog() {
        assertThat(provider.name()).isEqualTo("fake");
    }
}

package com.franco.opsengine.evaluation;

import com.franco.opsengine.evaluation.application.LeastLoadedStrategy;
import com.franco.opsengine.evaluation.application.ProviderStrategy;
import com.franco.opsengine.decision.adapter.fake.FakeDecisionProvider;
import com.franco.opsengine.evaluation.domain.EvaluationRoster;
import com.franco.opsengine.evaluation.domain.EvaluationStrategy;
import com.franco.opsengine.evaluation.domain.Evaluator;
import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.evaluation.domain.StrategyReport;
import com.franco.opsengine.routing.domain.RoutingPolicy;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluatorTest {

    private static final Skill BILLING = new Skill("BILLING");
    private static final Skill ACCESS = new Skill("ACCESS_IDENTITY");
    private static final List<Skill> SKILLS = List.of(BILLING, ACCESS);

    private static final Map<Skill, String> CATALOG = new LinkedHashMap<>(Map.of(
            BILLING, "Cobros, facturas, reembolsos y problemas con los pagos.",
            ACCESS, "Inicio de sesión, contraseñas, permisos y accesos de usuarios."));

    private static final RoutingPolicy POLICY = new RoutingPolicy(
            "test.1", 0.7, 0.6, 0.7, 0.6, Duration.ofMinutes(30));

    private static EvaluationRoster roster() {
        return EvaluationRoster.forSkills(SKILLS);
    }

    private static LabelledTicket ticket(String id, String title, String description, Skill expected) {
        return new LabelledTicket(id, title, description, expected);
    }

    /** Estrategia de prueba: siempre deriva. Sirve para fijar el caso extremo. */
    private static EvaluationStrategy alwaysReviews() {
        return new EvaluationStrategy() {
            @Override
            public String name() {
                return "siempre deriva";
            }

            @Override
            public Decision decide(LabelledTicket ticket, EvaluationRoster roster) {
                return Decision.review("ALWAYS");
            }
        };
    }

    @Test
    void countsACorrectAssignment() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros duplicados", "Nos llegaron dos facturas", BILLING));

        Evaluator.Run run = Evaluator.run(
                new ProviderStrategy("fake", new FakeDecisionProvider(), CATALOG, POLICY),
                dataset, roster());

        assertThat(run.report().correct()).isEqualTo(1);
        assertThat(run.report().silentErrors()).isZero();
        assertThat(run.report().accuracy()).isEqualTo(1.0);
    }

    @Test
    void anAssignmentToTheWrongSkillIsASilentError() {
        // El texto habla de facturas pero la etiqueta dice que era de accesos: el motor
        // asigna igual y nadie lo revisa. Es justo el caso que la métrica tiene que atrapar.
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros duplicados", "Nos llegaron dos facturas", ACCESS));

        Evaluator.Run run = Evaluator.run(
                new ProviderStrategy("fake", new FakeDecisionProvider(), CATALOG, POLICY),
                dataset, roster());

        assertThat(run.report().silentErrors()).isEqualTo(1);
        assertThat(run.report().correct()).isZero();
        assertThat(run.cases().getFirst().outcome())
                .isEqualTo(Evaluator.CaseResult.Outcome.SILENT_ERROR);
    }

    @Test
    void aTicketThatMatchesNothingIsSentToReviewAndIsNotASilentError() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Consulta", "Quisiera saber el horario de atención", BILLING));

        Evaluator.Run run = Evaluator.run(
                new ProviderStrategy("fake", new FakeDecisionProvider(), CATALOG, POLICY),
                dataset, roster());

        assertThat(run.report().sentToReview()).isEqualTo(1);
        assertThat(run.report().silentErrors()).isZero();
        assertThat(run.report().byReviewReason()).containsKey("SKILL_NOT_RECOGNIZED");
    }

    @Test
    void aStrategyThatAlwaysDefersHasNoSilentErrorsAndNoAccuracy() {
        // El caso extremo que obliga a leer las dos columnas juntas.
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros", "Dos facturas", BILLING),
                ticket("EV-02", "Acceso", "No puedo entrar", ACCESS));

        StrategyReport report = Evaluator.run(alwaysReviews(), dataset, roster()).report();

        assertThat(report.silentErrors()).isZero();
        assertThat(report.accuracy()).isZero();
        assertThat(report.reviewRate()).isEqualTo(1.0);
        assertThat(report.precisionWhenAssigned()).isZero();
    }

    @Test
    void theBaselineAssignsEverythingAndPaysForItInSilentErrors() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros", "Dos facturas", BILLING),
                ticket("EV-02", "Acceso", "No puedo entrar", ACCESS),
                ticket("EV-03", "Cobros", "Otra factura", BILLING));

        StrategyReport report = Evaluator.run(new LeastLoadedStrategy(), dataset, roster()).report();

        assertThat(report.sentToReview()).isZero();
        assertThat(report.correct() + report.silentErrors()).isEqualTo(3);
        // Repartir sin leer no puede acertar siempre con dos competencias distintas en juego.
        assertThat(report.silentErrors()).isPositive();
    }

    @Test
    void theBaselineSpreadsTheLoadInsteadOfPilingItOnOne() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "A", "texto", BILLING),
                ticket("EV-02", "B", "texto", BILLING),
                ticket("EV-03", "C", "texto", BILLING),
                ticket("EV-04", "D", "texto", BILLING));
        EvaluationRoster roster = roster();

        Evaluator.run(new LeastLoadedStrategy(), dataset, roster);

        assertThat(roster.loads().values()).containsOnly(2);
    }

    @Test
    void theOutcomesAlwaysAddUpToTheDatasetSize() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros duplicados", "Nos llegaron dos facturas", BILLING),
                ticket("EV-02", "Consulta", "Quisiera saber el horario", BILLING),
                ticket("EV-03", "Acceso", "No puedo iniciar sesión con mi contraseña", ACCESS));

        StrategyReport report = Evaluator.run(
                new ProviderStrategy("fake", new FakeDecisionProvider(), CATALOG, POLICY),
                dataset, roster()).report();

        assertThat(report.correct() + report.silentErrors() + report.sentToReview())
                .isEqualTo(3);
    }

    @Test
    void everyCaseIsReportedIndividuallySoAMissCanBeInspected() {
        List<LabelledTicket> dataset = List.of(
                ticket("EV-01", "Cobros duplicados", "Nos llegaron dos facturas", BILLING),
                ticket("EV-02", "Consulta", "Quisiera saber el horario", BILLING));

        Evaluator.Run run = Evaluator.run(
                new ProviderStrategy("fake", new FakeDecisionProvider(), CATALOG, POLICY),
                dataset, roster());

        assertThat(run.cases()).hasSize(2);
        assertThat(run.cases()).extracting(Evaluator.CaseResult::ticketId)
                .containsExactly("EV-01", "EV-02");
    }

    @Test
    void anEmptyDatasetCannotBeEvaluated() {
        assertThatThrownBy(() -> Evaluator.run(new LeastLoadedStrategy(), List.of(), roster()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aLabelIsMandatory() {
        assertThatThrownBy(() -> new LabelledTicket("EV-01", "T", "D", null))
                .isInstanceOf(NullPointerException.class);
    }
}

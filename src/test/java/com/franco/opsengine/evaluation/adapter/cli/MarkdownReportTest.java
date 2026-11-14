package com.franco.opsengine.evaluation.adapter.cli;

import com.franco.opsengine.evaluation.application.LeastLoadedStrategy;
import com.franco.opsengine.evaluation.domain.EvaluationRoster;
import com.franco.opsengine.evaluation.domain.Evaluator;
import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportTest {

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
    private static final Skill BILLING = new Skill("BILLING");

    @Test
    void saysTheTicketsAreRealWhenTheyAre() {
        String report = MarkdownReport.render(List.of(run()), 1, "reales anonimizados",
                "V8 (vigente)", new JevUsageTally(), 0.042, NOW);

        assertThat(report)
                .contains("Los tickets son reales anonimizados.")
                .doesNotContain("Los tickets son sintéticos");
    }

    @Test
    void saysTheTicketsAreSyntheticByDefault() {
        String report = MarkdownReport.render(List.of(run()), 1, "sintéticos",
                "V8 (vigente)", new JevUsageTally(), 0.042, NOW);

        assertThat(report).contains("Los tickets son sintéticos.");
    }

    @Test
    void showsTheCostOfTheTokensJevReported() {
        JevUsageTally usage = new JevUsageTally();
        usage.responded(1000, 20);

        String report = MarkdownReport.render(List.of(run()), 1, "reales anonimizados",
                "V8 (vigente)", usage, 0.042, NOW);

        assertThat(report)
                .contains("Tokens de entrada (facturables): 1000")
                .contains("Tokens de salida (gratis): 20")
                .contains("Costo estimado: $0.000042");
    }

    private static Evaluator.Run run() {
        List<LabelledTicket> dataset = List.of(new LabelledTicket("1", "Cobro", "Cobro duplicado", BILLING));
        return Evaluator.run(new LeastLoadedStrategy(), dataset, EvaluationRoster.forSkills(List.of(BILLING)));
    }
}

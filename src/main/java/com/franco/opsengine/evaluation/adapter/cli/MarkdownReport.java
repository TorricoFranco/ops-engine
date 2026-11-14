package com.franco.opsengine.evaluation.adapter.cli;

import com.franco.opsengine.evaluation.domain.Evaluator;
import com.franco.opsengine.evaluation.domain.StrategyReport;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arma el reporte en Markdown, listo para pegar en el README.
 *
 * <p>Incluye siempre el tamaño del dataset y de dónde salieron los tickets: un porcentaje sin
 * el denominador a la vista no se puede juzgar, y un reporte sobre tickets reales no puede
 * decir que son sintéticos.
 */
final class MarkdownReport {

    private MarkdownReport() {
    }

    /** @param origin cómo se describe a los tickets en "Los tickets son ...": "sintéticos", etc. */
    static String render(List<Evaluator.Run> runs, int datasetSize, String origin,
                         String catalogLabel, JevUsageTally usage, double pricePerMillion,
                         Instant generatedAt) {
        StringBuilder out = new StringBuilder();

        out.append("# Evaluación offline del ruteo\n\n");
        out.append("Generado el ").append(generatedAt).append(".\n\n");
        out.append("Dataset: **").append(datasetSize).append(" tickets** etiquetados a mano. ");
        out.append("Los tickets son ").append(origin).append(". ");
        out.append("El equipo contra el que se evalúa es sintético: ");
        out.append("estos números describen cómo se comporta el motor, no una operación real.\n\n");
        out.append("Catálogo de áreas: ").append(catalogLabel).append(".\n\n");

        out.append("## Resultados\n\n");
        out.append("| Estrategia | Aciertos | Errores silenciosos | A revisión | Precisión al asignar |\n");
        out.append("|---|---|---|---|---|\n");
        for (Evaluator.Run run : runs) {
            StrategyReport report = run.report();
            out.append("| ").append(report.strategy())
                    .append(" | ").append(count(report.correct(), report.total()))
                    .append(" | ").append(count(report.silentErrors(), report.total()))
                    .append(" | ").append(count(report.sentToReview(), report.total()))
                    .append(" | ").append(percent(report.precisionWhenAssigned()))
                    .append(" |\n");
        }

        out.append("\n## Cómo leer esto\n\n");
        out.append("- **Errores silenciosos** son asignaciones a quien no correspondía que nadie ");
        out.append("mandó a revisar. Son los peores: un ticket mal ruteado que cayó en la bandeja ");
        out.append("lo arregla una persona; uno que pasó derecho no lo descubre nadie.\n");
        out.append("- **A revisión** no es gratis: cuesta tiempo de una persona. Una estrategia que ");
        out.append("deriva todo no comete errores silenciosos y tampoco resuelve nada, así que las ");
        out.append("dos columnas se leen juntas.\n");
        out.append("- **Precisión al asignar** mide, de lo que la estrategia se animó a decidir, ");
        out.append("cuánto acertó.\n");

        for (Evaluator.Run run : runs) {
            Map<String, Integer> reasons = run.report().byReviewReason();
            if (reasons.isEmpty()) {
                continue;
            }
            out.append("\n### Por qué derivó ").append(run.report().strategy()).append("\n\n");
            reasons.forEach((reason, count) ->
                    out.append("- `").append(reason).append("`: ").append(count).append("\n"));
        }

        if (usage.calls() > 0) {
            out.append("\n## Costo de Jev\n\n");
            out.append("- Llamadas con respuesta: ").append(usage.calls()).append("\n");
            out.append("- Tokens de entrada (facturables): ").append(usage.inputTokens()).append("\n");
            out.append("- Tokens de salida (gratis): ").append(usage.outputTokens()).append("\n");
            out.append("- Costo estimado: $").append(String.format(Locale.ROOT, "%.6f", usage.costUsd(pricePerMillion)))
                    .append(", a $").append(pricePerMillion).append(" por millón de tokens de entrada.\n\n");
            out.append("El costo real es el que muestra la cuenta de TypeSafe. ");
            out.append("Esta cifra sale de los tokens que reporta la API.\n");
        }

        List<Evaluator.CaseResult> misses = runs.getLast().cases().stream()
                .filter(c -> c.outcome() != Evaluator.CaseResult.Outcome.CORRECT)
                .toList();
        if (!misses.isEmpty()) {
            out.append("\n## Casos que la última estrategia no resolvió bien\n\n");
            out.append("| Caso | Esperado | Asignado | Motivo |\n|---|---|---|---|\n");
            for (Evaluator.CaseResult miss : misses) {
                out.append("| ").append(miss.ticketId())
                        .append(" | ").append(miss.expectedSkill().code())
                        .append(" | ").append(miss.actualSkill() == null ? "—" : miss.actualSkill().code())
                        .append(" | ").append(miss.reviewReason() == null ? "error silencioso" : miss.reviewReason())
                        .append(" |\n");
            }
        }

        return out.toString();
    }

    private static String count(int value, int total) {
        return "%d (%s)".formatted(value, percent((double) value / total));
    }

    private static String percent(double ratio) {
        return String.format(Locale.ROOT, "%.0f%%", ratio * 100);
    }
}

package com.franco.opsengine.decision.adapter.fake;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.ChoiceAnswer;
import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.ScoreAnswer;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.Skill;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Proveedor de juicios sin red: cuenta coincidencias de palabras entre el ticket y la
 * descripción de cada competencia.
 *
 * <p>No imita al modelo ni pretende acertar: existe para que el sistema se pueda desarrollar,
 * demostrar y testear sin clave ni presupuesto, y para que la evaluación offline tenga una
 * línea de base contra la cual comparar. Es determinista a propósito: el mismo ticket da
 * siempre el mismo juicio, así los tests no dependen del azar.
 */
public class FakeDecisionProvider implements DecisionProvider {

    public static final String NAME = "fake";

    private static final int URGENCY_LEVELS = 5;
    private static final double SMOOTHING = 0.25;
    private static final Set<String> STOP_WORDS = Set.of(
            "de", "la", "el", "los", "las", "un", "una", "y", "o", "en", "con", "para", "por",
            "del", "al", "que", "se", "su", "sus", "es", "the", "a", "of", "to", "and");

    private static final List<String> OUTAGE_WORDS = List.of(
            "caido", "caida", "no funciona", "fuera de servicio", "inaccesible", "timeout", "error 500");
    private static final List<String> MANY_CUSTOMERS_WORDS = List.of(
            "varios clientes", "multiples clientes", "todos los clientes", "varias cuentas", "masivo");
    private static final List<String> URGENT_WORDS = List.of(
            "urgente", "bloqueado", "bloqueante", "critico", "cierre del dia", "cuanto antes", "ya mismo");

    @Override
    public TicketJudgement judge(TicketContext context) {
        String text = normalize(context.title() + " " + context.description());

        Map<String, Double> scores = scoreSkills(text, context.candidateSkills());
        Map<String, Double> probabilities = toProbabilities(scores);
        String best = probabilities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();

        boolean anyMatch = scores.values().stream().anyMatch(score -> score > 0);
        double topProbability = probabilities.get(best);

        return new TicketJudgement(
                new ChoiceAnswer(best, topProbability, probabilities),
                new NoulAnswer(anyMatch ? 0.85 : 0.2),
                urgency(text),
                new NoulAnswer(contains(text, MANY_CUSTOMERS_WORDS) ? 0.9 : 0.1),
                new NoulAnswer(contains(text, OUTAGE_WORDS) ? 0.9 : 0.1));
    }

    @Override
    public String name() {
        return NAME;
    }

    private static Map<String, Double> scoreSkills(String text, Map<Skill, String> candidates) {
        Map<String, Double> scores = new LinkedHashMap<>();
        candidates.entrySet().stream()
                // Orden estable: con empate siempre gana la misma competencia.
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Skill::code)))
                .forEach(entry -> {
                    String skillText = normalize(entry.getKey().code().replace('_', ' ') + " " + entry.getValue());
                    double hits = 0;
                    for (String word : skillText.split(" ")) {
                        if (word.length() > 3 && !STOP_WORDS.contains(word) && text.contains(word)) {
                            hits++;
                        }
                    }
                    scores.put(entry.getKey().code(), hits);
                });
        return scores;
    }

    /**
     * Suaviza para que ninguna opción quede en cero y las probabilidades sumen 1.
     *
     * <p>El suavizado es chico a propósito. Con uno más fuerte, una coincidencia clara queda
     * repartida entre todas las opciones y nunca supera los umbrales de ruteo: el proveedor
     * mandaría todo a revisión y no serviría ni para la demo ni como línea de base.
     */
    private static Map<String, Double> toProbabilities(Map<String, Double> scores) {
        double total = scores.values().stream().mapToDouble(Double::doubleValue).sum()
                + scores.size() * SMOOTHING;
        Map<String, Double> probabilities = new LinkedHashMap<>();
        scores.forEach((skill, score) -> probabilities.put(skill, (score + SMOOTHING) / total));
        return probabilities;
    }

    private static ScoreAnswer urgency(String text) {
        int level = 1;
        if (contains(text, URGENT_WORDS)) {
            level += 2;
        }
        if (contains(text, OUTAGE_WORDS)) {
            level += 1;
        }
        level = Math.min(level, URGENCY_LEVELS - 1);

        Map<Integer, Double> probabilities = new HashMap<>();
        for (int i = 0; i < URGENCY_LEVELS; i++) {
            probabilities.put(i, i == level ? 0.6 : 0.1);
        }
        return new ScoreAnswer(level, URGENCY_LEVELS, probabilities);
    }

    private static boolean contains(String text, List<String> needles) {
        return needles.stream().anyMatch(text::contains);
    }

    /**
     * Minúsculas, sin acentos y sin puntuación, con los espacios colapsados.
     *
     * <p>Quitar la puntuación importa: sin eso se buscaría "cobros," contra un texto que dice
     * "cobros" y la coincidencia se perdería.
     */
    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(decomposed.length());
        boolean lastWasSpace = true;
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
                lastWasSpace = false;
            } else if (!lastWasSpace) {
                out.append(' ');
                lastWasSpace = true;
            }
        }
        return out.toString().trim();
    }
}

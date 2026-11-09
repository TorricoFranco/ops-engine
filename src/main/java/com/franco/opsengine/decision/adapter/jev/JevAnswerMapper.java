package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.domain.ChoiceAnswer;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.ScoreAnswer;
import com.franco.opsengine.decision.domain.TicketJudgement;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce la respuesta cruda de Jev a los tipos del dominio.
 *
 * <p>Una respuesta incompleta o mal formada se trata como una falla, no como un juicio con
 * huecos: es preferible mandar el ticket a revisión antes que rutearlo con datos a medias.
 */
final class JevAnswerMapper {

    private JevAnswerMapper() {
    }

    static TicketJudgement toJudgement(JevResponse response) {
        if (response.answers() == null) {
            throw new DecisionUnavailableException("la respuesta de Jev no trae answers");
        }
        return new TicketJudgement(
                choice(response, JevQuestions.SKILL),
                noul(response, JevQuestions.SKILL_RECOGNIZED),
                score(response, JevQuestions.URGENCY),
                noul(response, JevQuestions.MULTIPLE_CUSTOMERS),
                noul(response, JevQuestions.SERVICE_UNAVAILABLE),
                noul(response, JevQuestions.ENOUGH_INFORMATION));
    }

    private static JevResponse.Answer answer(JevResponse response, String id) {
        JevResponse.Answer answer = response.answers().get(id);
        if (answer == null) {
            throw new DecisionUnavailableException("falta la respuesta a " + id);
        }
        return answer;
    }

    private static ChoiceAnswer choice(JevResponse response, String id) {
        JevResponse.Answer answer = answer(response, id);
        if (answer.choice() == null || answer.probabilities() == null) {
            throw new DecisionUnavailableException("la respuesta a " + id + " no es un choice válido");
        }
        // Un choice sin confidence se trata como la probabilidad de la opción elegida.
        double confidence = answer.confidence() != null
                ? answer.confidence()
                : answer.probabilities().getOrDefault(answer.choice(), 0.0);

        return wrapInvalid(id, () -> new ChoiceAnswer(answer.choice(), confidence, answer.probabilities()));
    }

    private static NoulAnswer noul(JevResponse response, String id) {
        JevResponse.Answer answer = answer(response, id);
        if (answer.noul() == null) {
            throw new DecisionUnavailableException("la respuesta a " + id + " no es un noul válido");
        }
        return wrapInvalid(id, () -> new NoulAnswer(answer.noul()));
    }

    private static ScoreAnswer score(JevResponse response, String id) {
        JevResponse.Answer answer = answer(response, id);
        if (answer.score() == null || answer.probabilities() == null || answer.probabilities().isEmpty()) {
            throw new DecisionUnavailableException("la respuesta a " + id + " no es un score válido");
        }

        Map<Integer, Double> byLevel = new HashMap<>();
        answer.probabilities().forEach((level, probability) -> {
            try {
                byLevel.put(Integer.parseInt(level), probability);
            } catch (NumberFormatException e) {
                throw new DecisionUnavailableException(
                        "el score " + id + " trae un nivel que no es un índice: " + level, e);
            }
        });

        return wrapInvalid(id, () -> new ScoreAnswer(answer.score(), byLevel.size(), byLevel));
    }

    /** Las validaciones del dominio también son respuestas inválidas del proveedor. */
    private static <T> T wrapInvalid(String id, java.util.function.Supplier<T> mapping) {
        try {
            return mapping.get();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DecisionUnavailableException(
                    "la respuesta a " + id + " es inválida: " + e.getMessage(), e);
        }
    }
}

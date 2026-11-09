package com.franco.opsengine.decision.domain;

import com.franco.opsengine.shared.domain.Skill;

import java.util.Objects;

/**
 * Los juicios que devuelve el proveedor sobre un ticket. Son observaciones, no decisiones:
 * ninguna de estas respuestas asigna el ticket ni lo escala. Eso lo resuelven las reglas de
 * ruteo con estos valores más los datos duros del sistema.
 *
 * @param requiredSkill    competencia más probable entre las candidatas
 * @param skillRecognized  probabilidad de que el ticket corresponda a alguna de las candidatas.
 *                         Va aparte porque un choice siempre elige una opción, incluso cuando
 *                         ninguna encaja: esta pregunta es la que detecta ese caso.
 * @param urgency          posición en la escala de urgencia descrita al proveedor
 * @param multipleCustomersAffected probabilidad de que el texto indique más de un cliente afectado
 * @param serviceUnavailable        probabilidad de que el texto indique un servicio caído
 * @param enoughInformation         probabilidad de que se entienda qué le pasa al cliente. Va
 *                                  aparte de {@code skillRecognized} porque son problemas
 *                                  distintos: uno es no saber a quién mandarlo, el otro es no
 *                                  entender el texto, y cada uno se resuelve de otra manera
 */
public record TicketJudgement(
        ChoiceAnswer requiredSkill,
        NoulAnswer skillRecognized,
        ScoreAnswer urgency,
        NoulAnswer multipleCustomersAffected,
        NoulAnswer serviceUnavailable,
        NoulAnswer enoughInformation) {

    public TicketJudgement {
        Objects.requireNonNull(requiredSkill, "required skill answer is missing");
        Objects.requireNonNull(skillRecognized, "skill recognized answer is missing");
        Objects.requireNonNull(urgency, "urgency answer is missing");
        Objects.requireNonNull(multipleCustomersAffected, "multiple customers answer is missing");
        Objects.requireNonNull(serviceUnavailable, "service unavailable answer is missing");
        Objects.requireNonNull(enoughInformation, "enough information answer is missing");
    }

    public Skill suggestedSkill() {
        return new Skill(requiredSkill.value());
    }
}

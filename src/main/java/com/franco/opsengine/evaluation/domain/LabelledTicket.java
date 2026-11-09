package com.franco.opsengine.evaluation.domain;

import com.franco.opsengine.shared.domain.Skill;

import java.util.Objects;

/**
 * Un ticket del dataset de evaluación, con la competencia que de verdad le correspondía.
 *
 * <p>La etiqueta la pone una persona con criterio de soporte. No puede salir de las mismas
 * reglas que se quieren medir: si el sistema se evaluara contra su propia lógica, acertaría
 * siempre y el número no diría nada.
 */
public record LabelledTicket(String id, String title, String description, Skill expectedSkill) {

    public LabelledTicket {
        Objects.requireNonNull(expectedSkill, "the expected skill is the label: it cannot be missing");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("every case needs an id to be traceable");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("case " + id + " has no title");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("case " + id + " has no description");
        }
        id = id.trim();
        title = title.trim();
        description = description.trim();
    }
}

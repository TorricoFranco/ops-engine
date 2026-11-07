package com.franco.opsengine.decision.domain;

import com.franco.opsengine.shared.domain.Skill;

import java.util.Map;
import java.util.Objects;

/**
 * Lo que el proveedor ve de un ticket. Es a propósito lo que leería una persona: el texto del
 * reclamo y las competencias entre las que puede elegir.
 *
 * <p>Deliberadamente no incluye el impacto declarado ni el vencimiento del SLA. Son datos duros
 * que el sistema ya conoce, y las reglas los aplican por código. Mandarlos acá además podría
 * sesgar el juicio de urgencia hacia lo que escribió quien abrió el ticket.
 *
 * @param candidateSkills competencias disponibles, con una descripción de cada una. La
 *                        descripción se le muestra al modelo, así que explica la competencia en
 *                        lugar de repetir su código.
 */
public record TicketContext(String title, String description, Map<Skill, String> candidateSkills) {

    public TicketContext {
        Objects.requireNonNull(candidateSkills, "candidate skills are required");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (candidateSkills.size() < 2) {
            throw new IllegalArgumentException("choosing a skill needs at least two candidates");
        }
        candidateSkills.forEach((skill, text) -> {
            Objects.requireNonNull(skill, "a candidate skill cannot be null");
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("skill " + skill.code() + " needs a description");
            }
        });
        title = title.trim();
        description = description.trim();
        candidateSkills = Map.copyOf(candidateSkills);
    }
}

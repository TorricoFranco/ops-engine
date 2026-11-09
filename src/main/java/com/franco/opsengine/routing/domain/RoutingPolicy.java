package com.franco.opsengine.routing.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Umbrales de ruteo, con versión (RF-04).
 *
 * <p>La versión se guarda junto a cada decisión: sin ella no se puede reconstruir por qué el
 * sistema decidió lo que decidió, porque los umbrales cambian con el tiempo.
 *
 * <p>Los valores iniciales de 0,7 y 0,6 son los que sugiere la documentación de TypeSafe, no
 * valores medidos sobre estos datos. Se ajustan con el dataset de evaluación (fase 6).
 *
 * @param minSkillRecognized   debajo de esto, el ticket no parece de ninguna competencia
 * @param minSkillConfidence   probabilidad mínima de la competencia elegida
 * @param criticalNoul         umbral para tomar por cierto que hay caída o varios afectados
 * @param criticalUrgency      urgencia normalizada (0 a 1) a partir de la cual el caso pesa
 * @param slaAtRiskWindow      si falta menos que esto para el vencimiento, el caso es crítico
 *                             aunque el modelo no lo vea urgente (RF-05)
 * @param minEnoughInformation debajo de esto, el ticket no se entiende y se le piden datos al
 *                             cliente en lugar de ocupar a una persona
 */
public record RoutingPolicy(
        String version,
        double minSkillRecognized,
        double minSkillConfidence,
        double criticalNoul,
        double criticalUrgency,
        Duration slaAtRiskWindow,
        double minEnoughInformation) {

    public RoutingPolicy {
        Objects.requireNonNull(slaAtRiskWindow, "the SLA window is required");
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("the policy needs a version to be auditable");
        }
        requireProbability(minSkillRecognized, "minSkillRecognized");
        requireProbability(minSkillConfidence, "minSkillConfidence");
        requireProbability(criticalNoul, "criticalNoul");
        requireProbability(criticalUrgency, "criticalUrgency");
        requireProbability(minEnoughInformation, "minEnoughInformation");
        if (slaAtRiskWindow.isNegative()) {
            throw new IllegalArgumentException("the SLA window cannot be negative");
        }
    }

    private static void requireProbability(double value, String name) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be between 0 and 1, but was " + value);
        }
    }
}

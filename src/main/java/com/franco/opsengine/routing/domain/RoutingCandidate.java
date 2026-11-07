package com.franco.opsengine.routing.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;

import java.util.Objects;
import java.util.Set;

/**
 * Lo que las reglas necesitan saber de un empleado. Es una vista propia del módulo de ruteo:
 * así las reglas se prueban sin construir el aggregate completo y sin depender de él.
 */
public record RoutingCandidate(
        EmployeeId id,
        String name,
        Set<Skill> skills,
        boolean available,
        int openTickets,
        int maxOpenTickets,
        boolean allowedForCriticalIncidents) {

    public RoutingCandidate {
        Objects.requireNonNull(id, "candidate id is required");
        Objects.requireNonNull(skills, "candidate skills are required");
        skills = Set.copyOf(skills);
    }

    public boolean hasCapacity() {
        return openTickets < maxOpenTickets;
    }
}

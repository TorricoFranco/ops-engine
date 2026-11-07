package com.franco.opsengine.team.domain;

import com.franco.opsengine.shared.domain.TeamId;

import java.util.Objects;

/**
 * Equipo al que pertenece un empleado. Agrupa competencias por área de soporte.
 */
public class Team {

    private final TeamId id;
    private final String name;

    private Team(TeamId id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Team create(TeamId id, String name) {
        Objects.requireNonNull(id, "team id is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("team name is required");
        }
        return new Team(id, name.trim());
    }

    public TeamId id() {
        return id;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Team that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

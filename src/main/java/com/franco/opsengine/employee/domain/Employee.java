package com.franco.opsengine.employee.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import com.franco.opsengine.shared.domain.TeamId;

import java.util.Objects;
import java.util.Set;

/**
 * Empleado que puede recibir tickets. Conoce sus skills, su disponibilidad y su carga actual.
 * La decisión de ruteo vive en otro módulo: acá solo se responde si puede tomar un ticket.
 */
public class Employee {

    private final EmployeeId id;
    private final String name;
    private final TeamId teamId;
    private final Set<Skill> skills;
    private final int maxOpenTickets;
    private Availability availability;
    private int openTickets;

    private Employee(EmployeeId id, String name, TeamId teamId, Set<Skill> skills, int maxOpenTickets,
                     Availability availability, int openTickets) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.skills = skills;
        this.maxOpenTickets = maxOpenTickets;
        this.availability = availability;
        this.openTickets = openTickets;
    }

    public static Employee create(EmployeeId id, String name, TeamId teamId, Set<Skill> skills, int maxOpenTickets) {
        return rebuild(id, name, teamId, skills, maxOpenTickets, Availability.AVAILABLE, 0);
    }

    /**
     * Reconstruye un empleado ya existente. Lo usa la capa de persistencia; el resto del
     * código crea empleados nuevos con {@link #create}.
     */
    public static Employee rebuild(EmployeeId id, String name, TeamId teamId, Set<Skill> skills, int maxOpenTickets,
                                   Availability availability, int openTickets) {
        Objects.requireNonNull(id, "employee id is required");
        Objects.requireNonNull(teamId, "team id is required");
        Objects.requireNonNull(skills, "skills are required");
        Objects.requireNonNull(availability, "availability is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (skills.isEmpty()) {
            throw new IllegalArgumentException("an employee needs at least one skill");
        }
        if (maxOpenTickets <= 0) {
            throw new IllegalArgumentException("max open tickets must be positive");
        }
        if (openTickets < 0) {
            throw new IllegalArgumentException("open tickets cannot be negative");
        }
        return new Employee(id, name.trim(), teamId, Set.copyOf(skills), maxOpenTickets, availability, openTickets);
    }

    public boolean canTake(Skill requiredSkill) {
        Objects.requireNonNull(requiredSkill, "required skill is required");
        return availability == Availability.AVAILABLE
                && skills.contains(requiredSkill)
                && openTickets < maxOpenTickets;
    }

    public void takeTicket(Skill requiredSkill) {
        if (!canTake(requiredSkill)) {
            throw new IllegalStateException(
                    "employee " + id.value() + " cannot take a ticket requiring " + requiredSkill.code());
        }
        openTickets++;
    }

    public void releaseTicket() {
        if (openTickets == 0) {
            throw new IllegalStateException("employee " + id.value() + " has no open tickets to release");
        }
        openTickets--;
    }

    public void changeAvailability(Availability newAvailability) {
        this.availability = Objects.requireNonNull(newAvailability, "availability is required");
    }

    public EmployeeId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TeamId teamId() {
        return teamId;
    }

    public Set<Skill> skills() {
        return skills;
    }

    public int maxOpenTickets() {
        return maxOpenTickets;
    }

    public Availability availability() {
        return availability;
    }

    public int openTickets() {
        return openTickets;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Employee that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

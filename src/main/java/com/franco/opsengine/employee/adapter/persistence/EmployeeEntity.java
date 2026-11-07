package com.franco.opsengine.employee.adapter.persistence;

import com.franco.opsengine.employee.domain.Availability;
import com.franco.opsengine.employee.domain.Clearance;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class EmployeeEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Availability availability;

    @Column(name = "max_open_tickets", nullable = false)
    private int maxOpenTickets;

    @Column(name = "open_tickets", nullable = false)
    private int openTickets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Clearance clearance;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill_code", nullable = false, length = 50)
    private Set<String> skills = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected EmployeeEntity() {
        // requerido por JPA
    }

    EmployeeEntity(UUID id, String name, UUID teamId, Availability availability, int maxOpenTickets,
                   int openTickets, Set<String> skills, Clearance clearance, long version) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.availability = availability;
        this.maxOpenTickets = maxOpenTickets;
        this.openTickets = openTickets;
        this.skills = new HashSet<>(skills);
        this.clearance = clearance;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    UUID getTeamId() {
        return teamId;
    }

    Availability getAvailability() {
        return availability;
    }

    int getMaxOpenTickets() {
        return maxOpenTickets;
    }

    int getOpenTickets() {
        return openTickets;
    }

    Set<String> getSkills() {
        return skills;
    }

    Clearance getClearance() {
        return clearance;
    }

    long getVersion() {
        return version;
    }
}

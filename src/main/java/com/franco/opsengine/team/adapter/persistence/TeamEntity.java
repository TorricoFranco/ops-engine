package com.franco.opsengine.team.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "teams")
public class TeamEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    protected TeamEntity() {
        // requerido por JPA
    }

    TeamEntity(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }
}

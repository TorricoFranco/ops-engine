package com.franco.opsengine.routing.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skills")
public class SkillEntity {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    protected SkillEntity() {
        // requerido por JPA
    }

    String getCode() {
        return code;
    }

    String getDescription() {
        return description;
    }

    String getDisplayName() {
        return displayName;
    }
}

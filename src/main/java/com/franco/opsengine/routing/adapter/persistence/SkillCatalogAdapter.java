package com.franco.opsengine.routing.adapter.persistence;

import com.franco.opsengine.routing.application.SkillCatalog;
import com.franco.opsengine.shared.domain.Skill;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
class SkillCatalogAdapter implements SkillCatalog {

    private final SkillJpaRepository jpa;

    SkillCatalogAdapter(SkillJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Map<Skill, String> all() {
        Map<Skill, String> catalog = new LinkedHashMap<>();
        jpa.findAll().forEach(entity -> catalog.put(new Skill(entity.getCode()), entity.getDescription()));
        return catalog;
    }
}

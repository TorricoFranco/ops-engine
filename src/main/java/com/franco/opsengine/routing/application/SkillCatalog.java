package com.franco.opsengine.routing.application;

import com.franco.opsengine.shared.domain.Skill;

import java.util.Map;

/**
 * Las competencias entre las que se puede elegir, con la descripción que ve el modelo.
 */
public interface SkillCatalog {

    Map<Skill, String> all();

    /**
     * El nombre que ve una persona en la consola. No es la descripción: esa es la que lee el
     * modelo, y es más larga.
     */
    Map<Skill, String> displayNames();
}

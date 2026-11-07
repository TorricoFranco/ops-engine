package com.franco.opsengine.shared.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Competencia técnica requerida por un ticket y poseída por un empleado.
 * El código se normaliza a mayúsculas: "access_identity" y "ACCESS_IDENTITY" son la misma skill.
 */
public record Skill(String code) {

    private static final Pattern VALID_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");

    public Skill {
        Objects.requireNonNull(code, "skill code is required");
        code = code.trim().toUpperCase(Locale.ROOT);
        if (!VALID_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("invalid skill code: " + code);
        }
    }
}

package com.franco.opsengine.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillTest {

    @Test
    void normalizesTheCodeToUpperCase() {
        assertThat(new Skill(" access_identity ").code()).isEqualTo("ACCESS_IDENTITY");
    }

    @Test
    void treatsDifferentCasingAsTheSameSkill() {
        assertThat(new Skill("billing")).isEqualTo(new Skill("BILLING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a b", "1ABC", "X", "BILLING-TEAM"})
    void rejectsInvalidCodes(String code) {
        assertThatThrownBy(() -> new Skill(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCode() {
        assertThatThrownBy(() -> new Skill(null))
                .isInstanceOf(NullPointerException.class);
    }
}

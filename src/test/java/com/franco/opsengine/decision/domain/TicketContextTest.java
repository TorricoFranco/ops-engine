package com.franco.opsengine.decision.domain;

import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketContextTest {

    private static final Map<Skill, String> SKILLS = Map.of(
            new Skill("BILLING"), "Cobros, facturas y reembolsos.",
            new Skill("ACCESS_IDENTITY"), "Inicio de sesión y permisos.");

    @Test
    void trimsTheTextItSendsToTheProvider() {
        TicketContext context = new TicketContext("  Cobros duplicados  ", "  Pasó dos veces  ", SKILLS);

        assertThat(context.title()).isEqualTo("Cobros duplicados");
        assertThat(context.description()).isEqualTo("Pasó dos veces");
    }

    @Test
    void needsAtLeastTwoCandidatesToBeWorthAsking() {
        Map<Skill, String> onlyOne = Map.of(new Skill("BILLING"), "Cobros.");

        assertThatThrownBy(() -> new TicketContext("Título", "Descripción", onlyOne))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two candidates");
    }

    @Test
    void everyCandidateNeedsADescriptionBecauseTheProviderReadsIt() {
        Map<Skill, String> missingText = Map.of(
                new Skill("BILLING"), "Cobros.",
                new Skill("ACCESS_IDENTITY"), "  ");

        assertThatThrownBy(() -> new TicketContext("Título", "Descripción", missingText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACCESS_IDENTITY");
    }

    @Test
    void rejectsAnEmptyDescription() {
        assertThatThrownBy(() -> new TicketContext("Título", "   ", SKILLS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }
}

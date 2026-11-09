package com.franco.opsengine.ticket.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    private static final Set<String> DOCUMENTED_TRANSITIONS = Set.of(
            "NUEVO->EVALUANDO",
            "EVALUANDO->ASIGNADO",
            "EVALUANDO->REVISION_REQUERIDA",
            "EVALUANDO->INFORMACION_INSUFICIENTE",
            "INFORMACION_INSUFICIENTE->EVALUANDO",
            "INFORMACION_INSUFICIENTE->CERRADO",
            "REVISION_REQUERIDA->ASIGNADO",
            "REVISION_REQUERIDA->CERRADO",
            "ASIGNADO->EN_PROGRESO",
            "EN_PROGRESO->RESUELTO",
            "RESUELTO->CERRADO");

    @Test
    void allowsExactlyTheDocumentedTransitions() {
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                boolean expected = DOCUMENTED_TRANSITIONS.contains(from + "->" + to);

                assertThat(from.canTransitionTo(to))
                        .as("transition %s -> %s", from, to)
                        .isEqualTo(expected);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void closedTicketsCannotMoveAnywhere(TicketStatus next) {
        assertThat(TicketStatus.CERRADO.canTransitionTo(next)).isFalse();
    }
}

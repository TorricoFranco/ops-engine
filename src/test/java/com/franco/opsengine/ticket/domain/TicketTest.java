package com.franco.opsengine.ticket.domain;

import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Skill ACCESS = new Skill("ACCESS_IDENTITY");
    private static final EmployeeId AGENT = EmployeeId.newId();

    private Ticket openTicket() {
        return Ticket.open(
                TicketId.newId(),
                "Cobros duplicados",
                "Desde la última actualización de la integración hay clientes con cobros duplicados",
                Impact.HIGH,
                ACCESS,
                NOW,
                NOW.plus(Duration.ofHours(4)));
    }

    @Test
    void opensInNuevoWithoutAssigneeOrReviewReason() {
        Ticket ticket = openTicket();

        assertThat(ticket.status()).isEqualTo(TicketStatus.NUEVO);
        assertThat(ticket.assignee()).isEmpty();
        assertThat(ticket.reviewReason()).isEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Ticket.open(
                TicketId.newId(), "   ", "descripción", Impact.LOW, ACCESS, NOW, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsTitleLongerThan200Characters() {
        assertThatThrownBy(() -> Ticket.open(
                TicketId.newId(), "a".repeat(201), "descripción", Impact.LOW, ACCESS, NOW, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200");
    }

    @Test
    void rejectsSlaDeadlineThatIsNotAfterCreation() {
        assertThatThrownBy(() -> Ticket.open(
                TicketId.newId(), "Título", "descripción", Impact.LOW, ACCESS, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SLA");
    }

    @Test
    void followsTheHappyPathToClosed() {
        Ticket ticket = openTicket();

        ticket.startEvaluation();
        ticket.assignTo(AGENT);
        ticket.startWork();
        ticket.resolve();
        ticket.close();

        assertThat(ticket.status()).isEqualTo(TicketStatus.CERRADO);
        assertThat(ticket.assignee()).contains(AGENT);
    }

    @Test
    void cannotAssignDirectlyFromNuevo() {
        Ticket ticket = openTicket();

        assertThatThrownBy(() -> ticket.assignTo(AGENT))
                .isInstanceOf(InvalidTicketTransitionException.class);
        assertThat(ticket.status()).isEqualTo(TicketStatus.NUEVO);
        assertThat(ticket.assignee()).isEmpty();
    }

    @Test
    void cannotResolveBeforeWorkStarts() {
        Ticket ticket = openTicket();
        ticket.startEvaluation();
        ticket.assignTo(AGENT);

        assertThatThrownBy(ticket::resolve)
                .isInstanceOf(InvalidTicketTransitionException.class);
        assertThat(ticket.status()).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test
    void sendsToReviewWhenTheEvaluationIsUncertain() {
        Ticket ticket = openTicket();
        ticket.startEvaluation();

        ticket.requireReview("La confianza del modelo está por debajo del umbral");

        assertThat(ticket.status()).isEqualTo(TicketStatus.REVISION_REQUERIDA);
        assertThat(ticket.reviewReason()).contains("La confianza del modelo está por debajo del umbral");
    }

    @Test
    void requiresANonBlankReviewReason() {
        Ticket ticket = openTicket();
        ticket.startEvaluation();

        assertThatThrownBy(() -> ticket.requireReview("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ticket.status()).isEqualTo(TicketStatus.EVALUANDO);
    }

    @Test
    void aSupervisorCanAssignAReviewedTicket() {
        Ticket ticket = openTicket();
        ticket.startEvaluation();
        ticket.requireReview("Caso ambiguo");

        ticket.assignTo(AGENT);

        assertThat(ticket.status()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(ticket.assignee()).contains(AGENT);
    }

    @Test
    void aReviewedTicketCanBeClosedWithoutAssignment() {
        Ticket ticket = openTicket();
        ticket.startEvaluation();
        ticket.requireReview("Duplicado de otro ticket");

        ticket.close();

        assertThat(ticket.status()).isEqualTo(TicketStatus.CERRADO);
        assertThat(ticket.assignee()).isEmpty();
    }
}

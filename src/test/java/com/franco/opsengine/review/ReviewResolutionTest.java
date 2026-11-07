package com.franco.opsengine.review;

import com.franco.opsengine.review.domain.ReviewResolution;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.ticket.domain.TicketId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewResolutionTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final TicketId TICKET = TicketId.newId();
    private static final EmployeeId AGENT = EmployeeId.newId();

    private static ReviewResolution resolution(ReviewResolution.Resolution kind, EmployeeId assignee,
                                               Boolean agreed, String reason) {
        return new ReviewResolution(UUID.randomUUID(), TICKET, UUID.randomUUID(), "supervisor",
                kind, assignee, agreed, reason, NOW);
    }

    @Test
    void anAgreementNeedsNoReason() {
        ReviewResolution resolved = resolution(
                ReviewResolution.Resolution.ASSIGNED, AGENT, true, null);

        assertThat(resolved.isOverride()).isFalse();
        assertThat(resolved.reasonForDisagreement()).isEmpty();
    }

    @Test
    void aDisagreementIsRecordedAsAnOverrideWithItsReason() {
        ReviewResolution resolved = resolution(ReviewResolution.Resolution.ASSIGNED, AGENT, false,
                "el ticket era de facturación, no de accesos");

        assertThat(resolved.isOverride()).isTrue();
        assertThat(resolved.reasonForDisagreement())
                .contains("el ticket era de facturación, no de accesos");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void aDisagreementWithoutAReasonIsRejected(String blankReason) {
        assertThatThrownBy(() -> resolution(
                ReviewResolution.Resolution.ASSIGNED, AGENT, false, blankReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void aDisagreementWithANullReasonIsRejected() {
        assertThatThrownBy(() -> resolution(ReviewResolution.Resolution.ASSIGNED, AGENT, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSuggestionMeansNeitherAgreementNorOverride() {
        // Cuando el proveedor no respondió no hay nada que contrastar. Contarlo como
        // desacuerdo inflaría la tasa de override con casos que no son del modelo.
        ReviewResolution resolved = resolution(
                ReviewResolution.Resolution.ASSIGNED, AGENT, null, null);

        assertThat(resolved.isOverride()).isFalse();
        assertThat(resolved.agreement()).isEmpty();
    }

    @Test
    void assigningFromReviewNeedsAnEmployee() {
        assertThatThrownBy(() -> resolution(ReviewResolution.Resolution.ASSIGNED, null, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee");
    }

    @Test
    void closingFromReviewCannotNameAnAssignee() {
        assertThatThrownBy(() -> resolution(ReviewResolution.Resolution.CLOSED, AGENT, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assignee");
    }

    @Test
    void theResolutionSaysWhoMadeIt() {
        assertThatThrownBy(() -> new ReviewResolution(UUID.randomUUID(), TICKET, null, "  ",
                ReviewResolution.Resolution.CLOSED, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trimsTheDisagreementReason() {
        ReviewResolution resolved = resolution(
                ReviewResolution.Resolution.CLOSED, null, false, "  duplicado  ");

        assertThat(resolved.reasonForDisagreement()).contains("duplicado");
    }
}

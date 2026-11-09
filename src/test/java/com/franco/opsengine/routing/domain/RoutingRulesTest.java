package com.franco.opsengine.routing.domain;

import com.franco.opsengine.decision.domain.ChoiceAnswer;
import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.ScoreAnswer;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.EmployeeId;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingRulesTest {

    private static final Skill BILLING = new Skill("BILLING");
    private static final Skill ACCESS = new Skill("ACCESS_IDENTITY");

    private static final RoutingPolicy POLICY = new RoutingPolicy(
            "test.1", 0.7, 0.6, 0.7, 0.6, Duration.ofMinutes(30), 0.5);

    private static final Duration PLENTY_OF_TIME = Duration.ofHours(8);

    // Ids fijos y ordenados, para que el desempate por id sea predecible en los tests.
    private static final EmployeeId ANA = new EmployeeId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
    private static final EmployeeId BRUNO = new EmployeeId(UUID.fromString("00000000-0000-0000-0000-00000000000b"));

    private static TicketJudgement judgement(double skillRecognized, double billingProbability,
                                             double urgencyLevel, double outage, double manyCustomers) {
        return judgement(skillRecognized, billingProbability, urgencyLevel, outage, manyCustomers, 0.95);
    }

    private static TicketJudgement judgement(double skillRecognized, double billingProbability,
                                             double urgencyLevel, double outage, double manyCustomers,
                                             double enoughInformation) {
        return new TicketJudgement(
                new ChoiceAnswer("BILLING", billingProbability,
                        Map.of("BILLING", billingProbability, "ACCESS_IDENTITY", 1 - billingProbability)),
                new NoulAnswer(skillRecognized),
                new ScoreAnswer(urgencyLevel, 5, Map.of((int) urgencyLevel, 1.0)),
                new NoulAnswer(manyCustomers),
                new NoulAnswer(outage),
                new NoulAnswer(enoughInformation));
    }

    /** Caso habitual: competencia clara, urgencia baja, nada raro. */
    private static TicketJudgement plainBillingTicket() {
        return judgement(0.95, 0.9, 1, 0.05, 0.05);
    }

    private static RoutingCandidate candidate(EmployeeId id, String name, Skill skill, boolean available,
                                              int open, int max, boolean critical) {
        return new RoutingCandidate(id, name, java.util.Set.of(skill), available, open, max, critical);
    }

    @Test
    void assignsTheSkilledAgentWithTheLeastLoad() {
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", BILLING, true, 4, 5, false),
                candidate(BRUNO, "Bruno", BILLING, true, 1, 5, false));

        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(), candidates, PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.Assign.class);
        assertThat(((RoutingOutcome.Assign) outcome).employeeId()).isEqualTo(BRUNO);
    }

    @Test
    void breaksTiesDeterministicallySoTheSameInputGivesTheSameAgent() {
        List<RoutingCandidate> candidates = List.of(
                candidate(BRUNO, "Bruno", BILLING, true, 2, 5, false),
                candidate(ANA, "Ana", BILLING, true, 2, 5, false));

        RoutingOutcome first = RoutingRules.decide(plainBillingTicket(), candidates, PLENTY_OF_TIME, POLICY);
        RoutingOutcome second = RoutingRules.decide(plainBillingTicket(), candidates, PLENTY_OF_TIME, POLICY);

        assertThat(((RoutingOutcome.Assign) first).employeeId()).isEqualTo(ANA);
        assertThat(((RoutingOutcome.Assign) second).employeeId()).isEqualTo(ANA);
    }

    @Test
    void recordsWhyEachCandidateWasDiscarded() {
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", ACCESS, true, 0, 5, false),
                candidate(BRUNO, "Bruno", BILLING, true, 0, 5, false));

        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(), candidates, PLENTY_OF_TIME, POLICY);

        assertThat(outcome.candidates()).hasSize(2);
        CandidateEvaluation ana = outcome.candidates().getFirst();
        assertThat(ana.eligible()).isFalse();
        assertThat(ana.rejection()).contains(RejectionReason.MISSING_SKILL);
    }

    @Test
    void carriesThePolicyVersionSoTheDecisionCanBeReconstructed() {
        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(),
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome.policyVersion()).isEqualTo("test.1");
    }

    @Test
    void sendsToReviewWhenTheTicketMatchesNoSkill() {
        RoutingOutcome outcome = RoutingRules.decide(judgement(0.2, 0.9, 1, 0.05, 0.05),
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.SendToReview.class);
        assertThat(((RoutingOutcome.SendToReview) outcome).reason())
                .isEqualTo(ReviewReason.SKILL_NOT_RECOGNIZED);
    }

    @Test
    void sendsToReviewWhenTheChosenSkillIsBelowTheThreshold() {
        RoutingOutcome outcome = RoutingRules.decide(judgement(0.95, 0.45, 1, 0.05, 0.05),
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(((RoutingOutcome.SendToReview) outcome).reason()).isEqualTo(ReviewReason.LOW_CONFIDENCE);
    }

    @Test
    void sendsToReviewWhenNobodyIsEligible() {
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", BILLING, false, 0, 5, false),
                candidate(BRUNO, "Bruno", BILLING, true, 5, 5, false));

        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(), candidates, PLENTY_OF_TIME, POLICY);

        assertThat(((RoutingOutcome.SendToReview) outcome).reason()).isEqualTo(ReviewReason.NO_ELIGIBLE_AGENT);
        assertThat(outcome.candidates()).extracting(c -> c.rejection().orElseThrow())
                .containsExactly(RejectionReason.UNAVAILABLE, RejectionReason.AT_CAPACITY);
    }

    @Test
    void sendsToReviewWhenThereAreNoCandidatesAtAll() {
        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(), List.of(), PLENTY_OF_TIME, POLICY);

        assertThat(((RoutingOutcome.SendToReview) outcome).reason()).isEqualTo(ReviewReason.NO_ELIGIBLE_AGENT);
    }

    @ParameterizedTest(name = "unavailable={0} open={1} skill={2} gives {3}")
    @CsvSource({
            "false, 0, BILLING,         UNAVAILABLE",
            "true,  5, BILLING,         AT_CAPACITY",
            "true,  0, ACCESS_IDENTITY, MISSING_SKILL"
    })
    void reportsTheFirstReasonThatDisqualifiesACandidate(boolean available, int open, String skill,
                                                         RejectionReason expected) {
        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(),
                List.of(candidate(ANA, "Ana", new Skill(skill), available, open, 5, false)),
                PLENTY_OF_TIME, POLICY);

        assertThat(outcome.candidates().getFirst().rejection()).contains(expected);
    }

    // --- Criticidad: dónde los datos duros le ganan al modelo ---

    @Test
    void anOutageWithHighUrgencyNeedsAnAgentWithClearance() {
        TicketJudgement critical = judgement(0.95, 0.9, 4, 0.95, 0.1);
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", BILLING, true, 0, 5, false));

        RoutingOutcome outcome = RoutingRules.decide(critical, candidates, PLENTY_OF_TIME, POLICY);

        assertThat(((RoutingOutcome.SendToReview) outcome).reason()).isEqualTo(ReviewReason.NO_ELIGIBLE_AGENT);
        assertThat(outcome.candidates().getFirst().rejection())
                .contains(RejectionReason.INSUFFICIENT_CLEARANCE);
    }

    @Test
    void anOutageWithHighUrgencyGoesToTheAgentWithClearance() {
        TicketJudgement critical = judgement(0.95, 0.9, 4, 0.95, 0.1);
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", BILLING, true, 0, 5, false),
                candidate(BRUNO, "Bruno", BILLING, true, 3, 5, true));

        RoutingOutcome outcome = RoutingRules.decide(critical, candidates, PLENTY_OF_TIME, POLICY);

        // Bruno tiene más carga, pero es el único habilitado: la restricción manda.
        assertThat(((RoutingOutcome.Assign) outcome).employeeId()).isEqualTo(BRUNO);
    }

    @Test
    void anExpiringSlaMakesTheTicketCriticalEvenWhenTheModelSeesNoUrgency() {
        // RF-05: el vencimiento real es un dato del sistema y pesa más que el tono del texto.
        TicketJudgement calm = plainBillingTicket();
        List<RoutingCandidate> candidates = List.of(
                candidate(ANA, "Ana", BILLING, true, 0, 5, false));

        RoutingOutcome withTime = RoutingRules.decide(calm, candidates, Duration.ofHours(8), POLICY);
        RoutingOutcome aboutToExpire = RoutingRules.decide(calm, candidates, Duration.ofMinutes(10), POLICY);

        assertThat(withTime).isInstanceOf(RoutingOutcome.Assign.class);
        assertThat(aboutToExpire).isInstanceOf(RoutingOutcome.SendToReview.class);
        assertThat(aboutToExpire.candidates().getFirst().rejection())
                .contains(RejectionReason.INSUFFICIENT_CLEARANCE);
    }

    @Test
    void anAlreadyOverdueSlaIsCriticalToo() {
        List<RoutingCandidate> candidates = List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false));

        RoutingOutcome outcome = RoutingRules.decide(
                plainBillingTicket(), candidates, Duration.ofMinutes(-5), POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.SendToReview.class);
    }

    @Test
    void highUrgencyAloneIsNotCritical() {
        // Urgencia alta sin caída ni varios afectados: sigue siendo un caso normal.
        TicketJudgement urgentButIsolated = judgement(0.95, 0.9, 4, 0.05, 0.05);

        RoutingOutcome outcome = RoutingRules.decide(urgentButIsolated,
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.Assign.class);
    }

    @Test
    void anOutageWithLowUrgencyIsNotCritical() {
        TicketJudgement outageButCalm = judgement(0.95, 0.9, 1, 0.95, 0.05);

        RoutingOutcome outcome = RoutingRules.decide(outageButCalm,
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.Assign.class);
    }

    @Test
    void manyAffectedCustomersCountTheSameAsAnOutage() {
        TicketJudgement widespread = judgement(0.95, 0.9, 4, 0.05, 0.95);

        RoutingOutcome outcome = RoutingRules.decide(widespread,
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome.candidates().getFirst().rejection())
                .contains(RejectionReason.INSUFFICIENT_CLEARANCE);
    }

    @Test
    void anUnintelligibleTicketAsksTheCustomerForDetailsInsteadOfGoingToReview() {
        // No es una derivación: no ocupa a una persona para que descubra que no dice nada.
        RoutingOutcome outcome = RoutingRules.decide(
                judgement(0.95, 0.9, 1, 0.05, 0.05, 0.1),
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)),
                PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.RequestMoreInformation.class);
        assertThat(outcome.rationale()).contains("detalle");
    }

    @Test
    void anUnintelligibleTicketIsCheckedBeforeAnythingElse() {
        // Si no se entiende el texto, la competencia que elija el modelo es ruido: no tiene
        // sentido evaluar candidatos ni umbrales de confianza sobre eso.
        RoutingOutcome outcome = RoutingRules.decide(
                judgement(0.1, 0.2, 1, 0.05, 0.05, 0.1),
                List.of(), PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.RequestMoreInformation.class);
    }

    @Test
    void aTicketJustAboveTheInformationThresholdIsRouted() {
        RoutingOutcome outcome = RoutingRules.decide(
                judgement(0.95, 0.9, 1, 0.05, 0.05, 0.5),
                List.of(candidate(ANA, "Ana", BILLING, true, 0, 5, false)),
                PLENTY_OF_TIME, POLICY);

        assertThat(outcome).isInstanceOf(RoutingOutcome.Assign.class);
    }

    @Test
    void theRationaleNamesTheAgentAndTheSkill() {
        RoutingOutcome outcome = RoutingRules.decide(plainBillingTicket(),
                List.of(candidate(ANA, "Ana", BILLING, true, 2, 5, false)), PLENTY_OF_TIME, POLICY);

        assertThat(outcome.rationale()).contains("Ana").contains("BILLING");
    }
}

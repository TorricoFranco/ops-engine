package com.franco.opsengine.decision.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTypesTest {

    @Test
    void aChoiceExposesTheProbabilityOfEveryOption() {
        ChoiceAnswer answer = new ChoiceAnswer("BILLING", 0.8, Map.of("BILLING", 0.8, "ACCESS", 0.2));

        assertThat(answer.probabilityOf("BILLING")).isEqualTo(0.8);
        assertThat(answer.probabilityOf("ACCESS")).isEqualTo(0.2);
        assertThat(answer.probabilityOf("UNKNOWN")).isZero();
    }

    @ParameterizedTest(name = "top probability 0.8 against threshold {0} is {1}")
    @CsvSource({"0.5,true", "0.8,true", "0.9,false"})
    void aChoiceComparesItsTopProbabilityAgainstAThreshold(double threshold, boolean expected) {
        ChoiceAnswer answer = new ChoiceAnswer("BILLING", 0.8, Map.of("BILLING", 0.8, "ACCESS", 0.2));

        assertThat(answer.isAtLeast(threshold)).isEqualTo(expected);
    }

    @Test
    void aChoiceCannotPickAnOptionItDidNotScore() {
        assertThatThrownBy(() -> new ChoiceAnswer("MISSING", 0.9, Map.of("BILLING", 1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MISSING");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01, Double.NaN})
    void probabilitiesOutsideZeroToOneAreRejected(double invalid) {
        assertThatThrownBy(() -> new NoulAnswer(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNoulNearTheMiddleIsUnsettledRatherThanYesOrNo() {
        NoulAnswer answer = new NoulAnswer(0.5);

        assertThat(answer.isYes(NoulAnswer.DEFAULT_YES)).isFalse();
        assertThat(answer.isNo(NoulAnswer.DEFAULT_NO)).isFalse();
        assertThat(answer.isUnsettled(NoulAnswer.DEFAULT_NO, NoulAnswer.DEFAULT_YES)).isTrue();
    }

    @ParameterizedTest(name = "a noul of {0} is yes={1} no={2}")
    @CsvSource({"0.95,true,false", "0.7,true,false", "0.3,false,true", "0.05,false,true"})
    void aNoulAppliesTheThresholdsAtTheirBoundaries(double probability, boolean yes, boolean no) {
        NoulAnswer answer = new NoulAnswer(probability);

        assertThat(answer.isYes(NoulAnswer.DEFAULT_YES)).isEqualTo(yes);
        assertThat(answer.isNo(NoulAnswer.DEFAULT_NO)).isEqualTo(no);
    }

    @Test
    void aScoreKeepsTheDistributionBecauseItsValueCanHideTwoPeaks() {
        // Dos picos en los extremos promedian al centro: el valor solo no distingue este caso.
        ScoreAnswer answer = new ScoreAnswer(2.0, 5, Map.of(0, 0.5, 4, 0.5));

        assertThat(answer.value()).isEqualTo(2.0);
        assertThat(answer.probabilityOfLevel(0)).isEqualTo(0.5);
        assertThat(answer.probabilityOfLevel(4)).isEqualTo(0.5);
        assertThat(answer.probabilityOfLevel(2)).isZero();
    }

    @Test
    void aScoreNormalizesToZeroOne() {
        assertThat(new ScoreAnswer(4.0, 5, Map.of(4, 1.0)).normalized()).isEqualTo(1.0);
        assertThat(new ScoreAnswer(0.0, 5, Map.of(0, 1.0)).normalized()).isZero();
    }

    @Test
    void aScoreOutsideItsScaleIsRejected() {
        assertThatThrownBy(() -> new ScoreAnswer(5.0, 5, Map.of(4, 1.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNoulRejectsThresholdsGivenInTheWrongOrder() {
        assertThatThrownBy(() -> new NoulAnswer(0.5).isUnsettled(0.8, 0.2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

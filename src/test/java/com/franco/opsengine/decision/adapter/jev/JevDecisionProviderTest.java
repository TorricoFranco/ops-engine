package com.franco.opsengine.decision.adapter.jev;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.NoulAnswer;
import com.franco.opsengine.decision.domain.TicketJudgement;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato contra la API de Jev, simulada con WireMock. Ningún test de esta clase sale a la
 * red real: la llamada contra la API de verdad vive aparte y no corre en CI.
 */
class JevDecisionProviderTest {

    private WireMockServer server;
    private JevDecisionProvider provider;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        provider = JevTestSupport.provider("http://localhost:" + server.port());
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + server.port();
    }

    private void respondWith(int status, String body) {
        server.stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    @Test
    void reportsTheTokensOfEveryAnsweredRequest() {
        respondWith(200, JevTestSupport.validResponse());
        List<int[]> tokens = new ArrayList<>();
        JevDecisionProvider counting = JevTestSupport.provider(baseUrl(),
                (input, output) -> tokens.add(new int[] {input, output}));

        counting.judge(JevTestSupport.context());

        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst()).containsExactly(180, 24);
    }

    @Test
    void mapsAValidResponseToTheDomainTypes() {
        respondWith(200, JevTestSupport.validResponse());

        TicketJudgement judgement = provider.judge(JevTestSupport.context());

        assertThat(judgement.suggestedSkill()).isEqualTo(new Skill("BILLING"));
        assertThat(judgement.requiredSkill().confidence()).isEqualTo(0.82);
        assertThat(judgement.requiredSkill().probabilityOf("ACCESS_IDENTITY")).isEqualTo(0.14);
        assertThat(judgement.skillRecognized().probability()).isEqualTo(0.94);
        assertThat(judgement.urgency().value()).isEqualTo(2.4);
        assertThat(judgement.urgency().levels()).isEqualTo(5);
        assertThat(judgement.urgency().probabilityOfLevel(3)).isEqualTo(0.4);
        assertThat(judgement.multipleCustomersAffected().isYes(NoulAnswer.DEFAULT_YES)).isTrue();
        assertThat(judgement.serviceUnavailable().isNo(NoulAnswer.DEFAULT_NO)).isTrue();
    }

    @Test
    void sendsTheKeyAsABearerTokenAndAllQuestionsInOneCall() {
        respondWith(200, JevTestSupport.validResponse());

        provider.judge(JevTestSupport.context());

        server.verify(postRequestedFor(urlEqualTo("/v1/systemone"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("jev-latest")))
                .withRequestBody(matchingJsonPath("$.questions.required_skill.type", equalTo("choice")))
                .withRequestBody(matchingJsonPath("$.questions.skill_recognized.type", equalTo("noul")))
                .withRequestBody(matchingJsonPath("$.questions.urgency.type", equalTo("score")))
                .withRequestBody(matchingJsonPath("$.questions.multiple_customers_affected.type", equalTo("noul")))
                .withRequestBody(matchingJsonPath("$.questions.service_unavailable.type", equalTo("noul"))));

        assertThat(server.getAllServeEvents()).hasSize(1);
    }

    @Test
    void sendsTheTicketTextAsNamedStateFields() {
        respondWith(200, JevTestSupport.validResponse());

        provider.judge(JevTestSupport.context());

        server.verify(postRequestedFor(urlEqualTo("/v1/systemone"))
                .withRequestBody(matchingJsonPath("$.state.ticket.titulo", equalTo("Cobros duplicados")))
                .withRequestBody(matchingJsonPath("$.state.ticket.descripcion",
                        equalTo("Nos cobraron dos veces este mes"))));
    }

    @Test
    void sendsEveryCandidateSkillAsAChoiceOption() {
        respondWith(200, JevTestSupport.validResponse());

        provider.judge(JevTestSupport.context());

        server.verify(postRequestedFor(urlEqualTo("/v1/systemone"))
                .withRequestBody(matchingJsonPath("$.questions.required_skill.criteria.BILLING"))
                .withRequestBody(matchingJsonPath("$.questions.required_skill.criteria.ACCESS_IDENTITY")));
    }

    @Test
    void aServerErrorBecomesDecisionUnavailableRatherThanAnAnswer() {
        respondWith(500, "{\"error\":\"boom\"}");

        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class);
    }

    @Test
    void aTimeoutBecomesDecisionUnavailable() {
        server.stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)
                        .withBody(JevTestSupport.validResponse())));

        JevDecisionProvider impatient = JevTestSupport.provider(
                JevTestSupport.properties(baseUrl(), 1, java.time.Duration.ofMillis(300)));

        assertThatThrownBy(() -> impatient.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class);
    }

    @Test
    void aBodyThatIsNotJsonBecomesDecisionUnavailable() {
        respondWith(200, "no soy json");

        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class);
    }

    @Test
    void aMissingAnswerBecomesDecisionUnavailable() {
        respondWith(200, """
                {
                  "model": "jev-latest",
                  "answers": {
                    "required_skill": {
                      "type": "choice",
                      "choice": "BILLING",
                      "confidence": 0.9,
                      "probabilities": {"BILLING": 0.9, "ACCESS_IDENTITY": 0.1}
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class)
                .hasMessageContaining("skill_recognized");
    }

    @Test
    void aProbabilityOutsideZeroToOneBecomesDecisionUnavailable() {
        respondWith(200, JevTestSupport.validResponse().replace("\"noul\": 0.94", "\"noul\": 1.4"));

        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class)
                .hasMessageContaining("skill_recognized");
    }

    @Test
    void aChoiceThatPicksAnOptionItDidNotScoreBecomesDecisionUnavailable() {
        respondWith(200, JevTestSupport.validResponse().replace("\"choice\": \"BILLING\"", "\"choice\": \"OTHER\""));

        assertThatThrownBy(() -> provider.judge(JevTestSupport.context()))
                .isInstanceOf(DecisionUnavailableException.class)
                .hasMessageContaining("required_skill");
    }

    @Test
    void identifiesItselfForTheDecisionLog() {
        assertThat(provider.name()).isEqualTo("jev");
    }
}

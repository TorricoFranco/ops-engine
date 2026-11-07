package com.franco.opsengine.decision.adapter.jev;

import com.franco.opsengine.decision.application.DecisionProvider;
import com.franco.opsengine.decision.domain.DecisionUnavailableException;
import com.franco.opsengine.decision.domain.TicketContext;
import com.franco.opsengine.decision.domain.TicketJudgement;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

/**
 * Adapter hacia la API de TypeSafe (Jev).
 *
 * <p>Las cinco preguntas van en un solo pedido: comparten el mismo estado, que se lee y se
 * cobra una vez, y el proveedor las responde en paralelo.
 *
 * <p>Cualquier falla termina en {@link DecisionUnavailableException}. Nunca se devuelve un
 * juicio neutro para tapar un error: si una llamada fallida se tratara como un "no", el ruteo
 * tomaría ruido por señal.
 */
public class JevDecisionProvider implements DecisionProvider {

    public static final String NAME = "jev";
    private static final Logger log = LoggerFactory.getLogger(JevDecisionProvider.class);

    private final RestClient client;
    private final JevProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public JevDecisionProvider(RestClient client, JevProperties properties,
                               CircuitBreaker circuitBreaker, Retry retry) {
        this.client = client;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public TicketJudgement judge(TicketContext context) {
        JevRequest request = new JevRequest(
                properties.model(),
                JevQuestions.state(context),
                JevQuestions.questions(context));

        Supplier<JevResponse> call = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> post(request)));

        long startedAt = System.nanoTime();
        try {
            JevResponse response = call.get();
            TicketJudgement judgement = JevAnswerMapper.toJudgement(response);
            log.info("Jev judgement received provider={} skill={} durationMs={}",
                    NAME, judgement.requiredSkill().value(), elapsedMs(startedAt));
            return judgement;
        } catch (CallNotPermittedException e) {
            // El circuito está abierto: ni siquiera se intenta la llamada.
            log.warn("Jev circuit breaker is open, skipping the call");
            throw new DecisionUnavailableException("el circuito hacia Jev está abierto", e);
        } catch (DecisionUnavailableException e) {
            log.warn("Jev judgement failed durationMs={} reason={}", elapsedMs(startedAt), e.getMessage());
            throw e;
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    private JevResponse post(JevRequest request) {
        try {
            JevResponse response = client.post()
                    .uri("/v1/systemone")
                    .body(request)
                    .retrieve()
                    .body(JevResponse.class);

            if (response == null) {
                throw new DecisionUnavailableException("Jev devolvió un cuerpo vacío");
            }
            return response;
        } catch (RestClientException e) {
            // El mensaje de la excepción puede traer el cuerpo de la respuesta, así que no se
            // encadena tal cual: solo la clase del error.
            throw new DecisionUnavailableException(
                    "falló la llamada a Jev: " + e.getClass().getSimpleName(), e);
        }
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

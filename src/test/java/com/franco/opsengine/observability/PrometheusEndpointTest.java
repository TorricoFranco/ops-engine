package com.franco.opsengine.observability;

import com.franco.opsengine.security.Role;
import com.franco.opsengine.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que los paneles y las alertas dan por sentado.
 *
 * <p>Un dashboard roto no falla: muestra un panel vacío, que es peor, porque se lee como
 * "no pasó nada". Estos tests fijan los nombres de las series de las que cuelgan las alertas
 * de `ops/prometheus/alerts.yml`.
 */
class PrometheusEndpointTest extends AbstractIntegrationTest {

    private static final Map<String, Object> TICKET = Map.of(
            "title", "Cobros duplicados",
            "description", "Desde la última actualización hay clientes con cobros duplicados",
            "impact", "HIGH",
            "requiredSkill", "BILLING",
            "slaMinutes", 240);

    private RestClient agent;

    @BeforeEach
    void setUp() {
        clearOperationalData();
        users.create("metrics-agent", Role.AGENT);
        agent = clientAs("metrics-agent");
    }

    @Test
    void exposesPrometheusWithoutCredentials() {
        // Prometheus raspa sin token. Puede hacerlo porque el puerto de management no se
        // publica fuera de la red de monitoreo, no porque el endpoint sea inofensivo.
        ResponseEntity<String> response = scrape();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void publishesTheQueueGaugesTheDashboardReads() {
        agent.post().uri("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .body(TICKET).retrieve().toBodilessEntity();

        String body = scrape().getBody();

        assertThat(body)
                .contains("ops_tickets_pending")
                .contains("ops_tickets_pending_oldest_seconds")
                .contains("ops_tickets_sla_at_risk")
                .contains("ops_tickets_awaiting_review")
                .contains("ops_outbox_pending");
    }

    @Test
    void countsTheTicketThatIsStillWaitingToBeRouted() {
        agent.post().uri("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .body(TICKET).retrieve().toBodilessEntity();

        // El worker está apagado en el perfil de tests, así que el ticket sigue en NUEVO.
        assertThat(valueOf(scrape().getBody(), "ops_tickets_pending")).isEqualTo(1.0);
    }

    @Test
    void publishesTheDecisionSeriesBeforeAnyTicketIsRouted() {
        // El worker está apagado en este perfil, así que acá no se decidió nada todavía. El
        // panel principal igual tiene que tener sus tres series en cero.
        String body = scrape().getBody();

        assertThat(body)
                .contains("ops_routing_decisions_total{application=\"ops-engine\"")
                .contains("outcome=\"assigned\"")
                .contains("outcome=\"sent_to_review\"")
                .contains("outcome=\"needs_more_information\"")
                .contains("ops_decision_judgement_seconds_count");
    }

    @Test
    void tagsEverySeriesWithTheApplication() {
        // Dos entornos apuntando al mismo Prometheus se mezclan sin esta etiqueta.
        assertThat(scrape().getBody()).contains("application=\"ops-engine\"");
    }

    @Test
    void exposesHttpLatencyPercentiles() {
        agent.get().uri("/api/tickets").retrieve().toBodilessEntity();

        // Sin histograma no hay p95: el promedio esconde justo la cola que importa.
        assertThat(scrape().getBody()).contains("http_server_requests_seconds_bucket");
    }

    private ResponseEntity<String> scrape() {
        return managementClient().get().uri("/actuator/prometheus").retrieve().toEntity(String.class);
    }

    /** Toma el valor de una serie sin etiquetas propias más allá de las comunes. */
    private static double valueOf(String body, String metric) {
        return body.lines()
                .filter(line -> line.startsWith(metric + "{"))
                .findFirst()
                .map(line -> Double.parseDouble(line.substring(line.lastIndexOf(' ') + 1)))
                .orElseThrow(() -> new AssertionError("no está la serie " + metric + " en:\n" + body));
    }
}

package com.franco.opsengine.routing.application;

import com.franco.opsengine.routing.domain.RoutingOutcome;

import java.time.Duration;

/**
 * Puerto de telemetría del ruteo.
 *
 * <p>Existe para que el caso de uso no importe Micrometer. No es ceremonia: el ruteo se
 * prueba sin Spring, y una implementación de dos líneas en los tests evita tener que montar
 * un registry solo para verificar una regla de negocio.
 *
 * <p>Todo lo que entra acá es agregado y anónimo. El detalle de cada decisión va a la
 * auditoría, que tiene acceso restringido; las métricas son públicas dentro de la red de
 * monitoreo y no deben permitir reconstruir un ticket (RNF-04).
 */
public interface RoutingMetrics {

    /**
     * Cuánto tardó el proveedor y si contestó. Incluye los reintentos, porque lo que importa
     * para la alerta es la espera real del ticket, no la de un intento aislado.
     */
    void judged(String provider, Duration took, boolean available);

    /** El final que le tocó al ticket. De acá sale la tasa de derivación a revisión. */
    void decided(RoutingOutcome outcome);

    /** Un intento que no terminó en decisión: la próxima pasada del worker lo reintenta. */
    void routingFailed(String reason);

    /** Implementación vacía para los tests que no miran telemetría. */
    RoutingMetrics NONE = new RoutingMetrics() {

        @Override
        public void judged(String provider, Duration took, boolean available) {
        }

        @Override
        public void decided(RoutingOutcome outcome) {
        }

        @Override
        public void routingFailed(String reason) {
        }
    };
}

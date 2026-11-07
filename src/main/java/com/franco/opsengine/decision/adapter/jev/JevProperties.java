package com.franco.opsengine.decision.adapter.jev;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param apiKey TYPESAFE_API_KEY. Solo desde variables de entorno y nunca se loguea.
 * @param model  alias del modelo, por ejemplo jev-latest.
 */
@ConfigurationProperties(prefix = "app.decision.jev")
public record JevProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts) {

    public JevProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "falta TYPESAFE_API_KEY: el proveedor jev no puede arrancar sin clave");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.decision.jev.base-url es obligatorio");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("app.decision.jev.model es obligatorio");
        }
        if (maxAttempts < 1) {
            throw new IllegalStateException("app.decision.jev.max-attempts debe ser al menos 1");
        }
    }
}

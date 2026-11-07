package com.franco.opsengine.support;

import com.franco.opsengine.security.TestUserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de los tests de integración: PostgreSQL real con Testcontainers y Flyway aplicando
 * las migraciones. No se usa H2, para que lo que se prueba sea el mismo motor de producción.
 *
 * <p>El contenedor se arranca una sola vez por JVM y se comparte entre todas las clases que
 * extienden esta. No se usa {@code @Testcontainers} con {@code @Container} a propósito: esa
 * combinación detiene el contenedor al terminar cada clase, y la siguiente se encontraba la
 * base caída.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestUserFactory.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected TestUserFactory users;

    protected RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    // Los tests inspeccionan el status; RestClient no debe lanzar por 4xx/5xx.
                })
                .build();
    }

    protected RestClient clientAs(String username) {
        String token = users.tokenFor(username);
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                })
                .build();
    }

    protected static int statusOf(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }
}

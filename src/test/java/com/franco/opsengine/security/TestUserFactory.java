package com.franco.opsengine.security;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Crea usuarios para los tests. Vive en el paquete `security` porque el repositorio y la
 * entidad son package-private: el resto de la aplicación no debe tocarlos.
 */
@TestComponent
public class TestUserFactory {

    public static final String PASSWORD = "test-password";

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    TestUserFactory(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Crea el usuario si no existe y devuelve su nombre. La contraseña es {@link #PASSWORD}. */
    public String create(String username, Role role) {
        if (!users.existsByUsername(username)) {
            users.save(new AppUserEntity(UUID.randomUUID(), username, encoder.encode(PASSWORD), role));
        }
        return username;
    }

    /** Token para un usuario ya creado. Falla si no existe, para no ocultar errores de setup. */
    public String tokenFor(String username) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("test user does not exist: " + username));
        return jwt.issue(user.getUsername(), user.getRole());
    }
}

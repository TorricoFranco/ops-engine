package com.franco.opsengine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Crea los usuarios de prueba del perfil `dev`. La contraseña se hashea acá en vez de
 * dejar un hash fijo en una migración, y estos usuarios no existen en ningún otro perfil.
 */
@Configuration
@Profile("dev")
class DevUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);
    private static final String DEV_PASSWORD = "devpassword";

    @Bean
    ApplicationRunner seedDevUsers(AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            for (Role role : Role.values()) {
                String username = role.name().toLowerCase();
                if (users.existsByUsername(username)) {
                    continue;
                }
                users.save(new AppUserEntity(
                        UUID.randomUUID(), username, encoder.encode(DEV_PASSWORD), role));
                log.info("Dev user created username={} role={}", username, role);
            }
        };
    }
}

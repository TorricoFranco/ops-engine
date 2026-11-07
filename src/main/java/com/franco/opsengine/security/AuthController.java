package com.franco.opsengine.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
@SecurityRequirements
class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record TokenResponse(String accessToken, String tokenType, Instant expiresAt, Role role) {
    }

    @PostMapping("/login")
    @Operation(summary = "Devuelve un JWT para usarlo como Bearer token")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var user = users.findByUsername(request.username());

        // Mismo mensaje para usuario inexistente y contraseña incorrecta: no se filtra
        // qué usuarios existen. Tampoco se loguea la contraseña.
        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPasswordHash())) {
            log.warn("Failed login attempt for username={}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Role role = user.get().getRole();
        return ResponseEntity.ok(new TokenResponse(
                jwt.issue(user.get().getUsername(), role), "Bearer", jwt.expiresAt(), role));
    }
}

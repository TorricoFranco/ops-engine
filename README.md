# ops-engine

Motor de operaciones para soporte técnico: ruteo inteligente de tickets, revisión humana, auditoría y evaluación de decisiones. La IA (Jev, de TypeSafe AI) es un componente controlado: sus sugerencias pasan por reglas explícitas y nunca son la autorización final.

> **Estado:** fase 2 completa (persistencia, API y seguridad). Ver [plan del MVP](docs/plan.md).

## Stack

- Java 21, Spring Boot 4.1.2, Maven
- PostgreSQL 17 con Flyway y Spring Data JPA
- Spring Security como resource server, con JWT propio y roles
- JUnit 5, AssertJ, Testcontainers y ArchUnit
- OpenAPI con springdoc, métricas con Actuator y Prometheus
- CI con GitHub Actions

## Requisitos

- JDK 21 o superior
- Maven
- Docker (para PostgreSQL)

## Cómo correrlo

```bash
docker compose up -d
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Health check: http://localhost:8080/actuator/health

Documentación de la API: http://localhost:8080/swagger-ui.html

El perfil `dev` carga equipos y empleados de ejemplo, y crea los usuarios `agent`, `supervisor` y `admin`. La contraseña de desarrollo está en `DevUserSeeder` y esos usuarios no existen en ningún otro perfil.

Fuera de `dev` hay que definir `JWT_SECRET` con al menos 32 bytes, o la aplicación no arranca:

```bash
JWT_SECRET=... mvn spring-boot:run
```

## Tests

```bash
mvn verify
```

## Documentación

- [Plan del MVP](docs/plan.md)
- [Logs y monitoreo](docs/monitoring.md)
- [ADR-001: Spring Boot con Java 21](docs/adr/ADR-001-spring-boot-java21.md)
- [ADR-002: Módulos verificados con ArchUnit](docs/adr/ADR-002-modulos-con-archunit.md)
- [ADR-003: Autenticación con JWT](docs/adr/ADR-003-autenticacion-jwt.md)
- [ADR-004: La aplicación corre en UTC](docs/adr/ADR-004-utc-en-toda-la-aplicacion.md)

## Datos

Los datos del proyecto son sintéticos. Las métricas muestran cómo se comporta el sistema, no resultados de una empresa real.

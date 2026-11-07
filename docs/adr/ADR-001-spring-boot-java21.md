# ADR-001: Spring Boot con Java 21 y Maven

- **Estado:** Aceptada
- **Fecha:** 2026-11-06

## Contexto

El proyecto es un motor de operaciones de soporte técnico pensado para un portfolio. Tiene que parecer un sistema empresarial real y demostrar backend Java con Spring y tests. El plan inicial usaba NestJS, pero el objetivo del proyecto es Java.

## Decisión

- **Java 21 (LTS)**, versión de largo soporte muy usada en proyectos empresariales.
- **Spring Boot 4.1.2**, la última versión estable publicada al momento de esta decisión. La 4.2.0 todavía es candidata a release.
- **Maven** como herramienta de build, por la legibilidad del `pom.xml` para quien revisa el proyecto.
- **JUnit 5 y AssertJ**, incluidos en `spring-boot-starter-test`.
- **CI en GitHub Actions** que ejecuta `mvn verify` en cada push a `main` y en cada pull request.

## Consecuencias

**Positivas**
- Ecosistema maduro: Spring Modulith, Resilience4j, Testcontainers y Flyway se integran sin fricción.
- La estructura del proyecto es reconocible para quien lo evalúa.

**Negativas**
- Maven es más verboso que Gradle.
- Java 25 ya es LTS y está instalado en la máquina de desarrollo. Se mantiene 21 porque es la versión más extendida en proyectos existentes. Puede revisarse en un ADR futuro.

## Alternativas consideradas

- **NestJS (stack original):** descartado porque el objetivo del proyecto es demostrar Java y Spring.
- **Gradle:** también válido. Se eligió Maven por la legibilidad del `pom.xml`.
- **Java 25:** LTS más nuevo, pero menos extendido todavía en proyectos existentes.

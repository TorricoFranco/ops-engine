# ops-engine

Motor de operaciones para soporte técnico: ruteo inteligente de tickets, revisión humana, auditoría y evaluación de decisiones. La IA (Jev, de TypeSafe AI) es un componente controlado: sus sugerencias pasan por reglas explícitas y nunca son la autorización final.

> **Estado:** MVP completo con consola web. Ver [plan del MVP](docs/plan.md).

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

Documentación de la API: http://localhost:8080/swagger-ui.html

Health check y métricas van por un puerto aparte, el 9091: son datos de operación y no se
sirven junto con la API (ADR-012).

- http://localhost:9091/actuator/health
- http://localhost:9091/actuator/prometheus

El perfil `dev` carga equipos y empleados de ejemplo, y crea los usuarios `agent`, `supervisor` y `admin`. La contraseña de desarrollo está en `DevUserSeeder` y esos usuarios no existen en ningún otro perfil.

Fuera de `dev` hay que definir `JWT_SECRET` con al menos 32 bytes, o la aplicación no arranca:

```bash
JWT_SECRET=... mvn spring-boot:run
```

## Tests

```bash
mvn verify
```

## Contribuir

`main` está protegida: los cambios entran por pull request desde `develop`, con el CI en verde. Las ramas de trabajo salen de `develop` y usan los prefijos `feat/`, `fix/`, `docs/`, `chore/` o `refactor/`.

## Documentación

- [Plan del MVP](docs/plan.md)
- [Logs y monitoreo](docs/monitoring.md)
- [ADR-001: Spring Boot con Java 21](docs/adr/ADR-001-spring-boot-java21.md)
- [ADR-002: Módulos verificados con ArchUnit](docs/adr/ADR-002-modulos-con-archunit.md)
- [ADR-003: Autenticación con JWT](docs/adr/ADR-003-autenticacion-jwt.md)
- [ADR-004: La aplicación corre en UTC](docs/adr/ADR-004-utc-en-toda-la-aplicacion.md)
- [ADR-005: `DecisionProvider` como puerto, y la IA nunca decide](docs/adr/ADR-005-decision-provider.md)
- [ADR-006: Outbox transaccional en lugar de un broker](docs/adr/ADR-006-outbox-sin-broker.md)
- [ADR-007: Optimistic locking con la versión en el aggregate](docs/adr/ADR-007-optimistic-locking.md)
- [ADR-008: Auditoría de decisiones y registro del desacuerdo humano](docs/adr/ADR-008-auditoria-de-decisiones.md)
- [ADR-009: Evaluación offline con dataset etiquetado a mano](docs/adr/ADR-009-evaluacion-offline.md)
- [ADR-010: El ruteo corre en segundo plano, fuera de la transacción](docs/adr/ADR-010-ruteo-asincrono.md)
- [ADR-011: Una consola que explica el motor sin leer código](docs/adr/ADR-011-consola-de-operaciones.md)
- [ADR-012: Métricas en un puerto aparte](docs/adr/ADR-012-observabilidad-con-prometheus.md)
- [ADR-013: Los tipos del frontend salen del OpenAPI](docs/adr/ADR-013-tipos-del-frontend-desde-openapi.md)
- [Cómo correr la evaluación](docs/evaluation/README.md)

## El papel de la IA

El modelo responde preguntas acotadas sobre el texto del ticket: qué competencia hace falta, cuánto le impide operar al cliente, si menciona varios afectados o un servicio caído. No decide a quién se asigna ni si hay que escalar: eso depende de permisos, del SLA y de la carga del equipo, y lo resuelven reglas explícitas en el código.

Si el proveedor falla, tarda de más o devuelve algo que no se entiende, el ticket va a revisión humana. Una llamada fallida nunca se convierte en un "no".

Cada decisión se puede explicar. La respuesta del ruteo dice a quién se asignó, qué candidatos se evaluaron, cuáles quedaron afuera y por qué motivo, y con qué versión de reglas se decidió:

```bash
curl -X POST localhost:8080/api/tickets/{id}/routing -H "Authorization: Bearer $TOKEN"
```

Hay restricciones que el modelo no puede saltear. Un incidente crítico solo va a alguien con habilitación, y el vencimiento real del SLA pesa más que la urgencia que se desprenda del texto.

Cada decisión queda guardada con sus probabilidades, la versión de reglas y los candidatos descartados, así que se puede reconstruir meses después:

```bash
curl localhost:8080/api/tickets/{id}/decisions -H "Authorization: Bearer $TOKEN"
```

Cuando un ticket va a revisión, el supervisor dice si coincidió con lo que sugirió el sistema y, si no, por qué. De ahí sale la tasa de override, en `/api/review/metrics`. Si el proveedor no respondió no hubo sugerencia que contrastar, y ese caso no cuenta como desacuerdo.

Por defecto el sistema usa un proveedor local sin red. Para usar Jev:

```bash
DECISION_PROVIDER=jev TYPESAFE_API_KEY=... mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## La consola

Un panel en Next.js que muestra cómo se decidió cada ticket, para entender el motor sin leer
código. La pantalla de detalle está partida en dos: lo que estimó el modelo y lo que decidió
el código, con cada candidato y su motivo de descarte.

```bash
docker compose up -d
```

```bash
DECISION_PROVIDER=jev mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
npm --prefix frontend run dev
```

El panel queda en http://localhost:3000. El perfil `dev` crea diez tickets ya ruteados, así
que hay algo que mirar desde el primer arranque.

## Monitoreo

`docker compose up -d` levanta también Prometheus y Grafana, con el tablero y las alertas
versionados en `ops/`.

- Grafana: http://localhost:3001 — tablero **ops-engine: motor de operaciones**, ya cargado.
- Prometheus: http://localhost:9090

El tablero tiene tres secciones: salud técnica (latencia y errores de la API), proveedor de
juicios (latencia, fallas y circuit breaker) y negocio (decisiones por resultado, derivación
a una persona, override humano y la cola abierta por estado).

Las alertas viven en [`ops/prometheus/alerts.yml`](ops/prometheus/alerts.yml) y usan las
mismas consultas que los paneles. Los umbrales son propuestas de `docs/monitoring.md`, no
valores calibrados contra tráfico real. Prometheus las evalúa y las muestra, pero no hay
Alertmanager: no se envían a ningún lado.

## Evaluación

El motor se mide contra una línea base sobre un conjunto de casos etiquetados a mano:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--evaluate"
```

La métrica principal son los **errores silenciosos**: asignaciones a quien no correspondía que
además nadie mandó a revisar. Derivar a una persona tampoco es gratis, así que las dos cifras
se leen juntas. Ver [docs/evaluation](docs/evaluation/README.md).

## Datos

Los datos del proyecto son sintéticos. Las métricas muestran cómo se comporta el sistema, no resultados de una empresa real.

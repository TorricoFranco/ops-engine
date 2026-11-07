# ops-engine

Motor de operaciones para soporte técnico: ruteo de tickets, revisión humana, auditoría y evaluación de decisiones. Es un proyecto de portfolio y el código tiene que parecer el de una empresa real.

Planes y decisiones viven en `docs/`. Si algo de este archivo contradice `docs/plan.md`, avisame antes de seguir.

## Estado

- Fase 0 completa: base del proyecto, CI, health check y ADR-001.
- Fase 1 completa: aggregates `Ticket` y `Employee`, skills, máquina de estados y tests de dominio.
- Fase 2 completa: Flyway + JPA, API REST de tickets, empleados y equipos, idempotencia, JWT con roles, OpenAPI y logs JSON. `mvn verify` pasa con 65 tests, 16 de integración con Testcontainers.
- Fase 3 completa: puerto `DecisionProvider`, `FakeDecisionProvider` determinista, adapter de Jev con reintentos y circuit breaker, y tests de contrato con WireMock. `mvn verify` pasa con 109 tests.
- Fase 4 completa: reglas de ruteo con candidatos y motivos de descarte, asignación con optimistic locking, outbox transaccional y fallback a revisión. `mvn verify` pasa con 142 tests.
- Fase 5 completa: auditoría append-only de cada decisión, bandeja de revisión, registro del desacuerdo humano y métricas de override. `mvn verify` pasa con 166 tests.
- Próxima: fase 6, evaluación offline con dataset etiquetado. Fases en `docs/plan.md`, sección 8.
- La IA da juicios, el código decide. Nunca preguntarle a Jev qué hacer con un ticket (ADR-005).
- La aplicación corre en UTC (ADR-004). Los módulos se verifican con ArchUnit, no con Spring Modulith (ADR-002).
- Skills propias en `.claude/skills/`: `adr-new` (crear ADRs) y `add-feature` (agregar funcionalidades con sus tests).

## Stack

- Java 21, Spring Boot 4.1.2, Maven.
- PostgreSQL 17 con Docker Compose. Desde la fase 2: Spring Data JPA y Flyway.
- Tests: JUnit 5, AssertJ, Mockito, Testcontainers, WireMock, ArchUnit.
- IA: TypeSafe Jev detrás del puerto `DecisionProvider`. Mientras no haya clave, usar `FakeDecisionProvider`.
- Frontend (fase 8): Next.js con TypeScript, Tailwind CSS y shadcn/ui. Tests con Jest, React Testing Library y Playwright. Jest solo va en el frontend.

## Comandos

- Levantar PostgreSQL: `docker compose up -d`
- Correr la app: `mvn spring-boot:run`
- Build completo con tests: `mvn verify`. Es lo mismo que corre el CI. Correrlo antes de dar algo por terminado.
- Un solo test: `mvn -Dtest=NombreDeClase test`
- Maven todavía no está en el PATH. Mientras tanto, usar `C:\Users\Franco\tools\apache-maven-3.9.16\bin\mvn`.

## Estructura

- Paquete raíz: `com.franco.opsengine`.
- Un paquete por módulo de negocio, por ejemplo `ticket`, `employee`, `routing`, `review`, `audit`, `decision`, `evaluation`.
- Dentro de cada módulo:
  - `domain`: entidades, value objects y reglas. Sin imports de Spring ni de JPA.
  - `application`: casos de uso.
  - `adapter`: web, persistencia y el adapter de Jev.
- Los módulos se comunican solo por su API pública.

## Reglas de código

- La lógica de negocio va en `domain`. Los controladores son finos y las entidades JPA no contienen reglas.
- Usar `record` para DTOs y value objects. Preferir inmutabilidad.
- Las transiciones de estado del ticket se validan en el dominio (ver `docs/plan.md`, sección 4).
- `Optional` solo como tipo de retorno. Nunca en parámetros ni en campos.
- Excepciones de dominio propias. El mapeo a HTTP vive en un único `@RestControllerAdvice`.
- Fechas con `java.time`. En persistencia se guarda `Instant` en UTC. La zona horaria se usa solo en presentación y en el calendario de SLA.
- Código e identificadores en inglés. Documentación, ADRs y mensajes de usuario en español.
- Sin comentarios que repitan lo que hace el código. Sí comentarios que expliquen un porqué no obvio.
- Logs con SLF4J. Nunca loguear el texto completo de un ticket, claves ni prompts (ver `docs/monitoring.md`).
- Las migraciones de Flyway van en `src/main/resources/db/migration`, con nombres `V<n>__descripcion.sql`. Una migración ya aplicada no se edita: se crea una nueva.

## Tests

- Cada regla de ruteo tiene tests unitarios parametrizados (`@ParameterizedTest`), incluidas las ramas de error.
- La integración con base de datos usa Testcontainers con PostgreSQL. No usar H2.
- Los tests automáticos nunca llaman a Jev. Usar WireMock o `FakeDecisionProvider`.
- Una llamada real a Jev va en un test marcado con `@Tag("live")`, que no corre en CI.
- Cada asignación tiene un test de concurrencia que demuestra que dos asignaciones simultáneas no se pisan.
- Nombres de test descriptivos en inglés, por ejemplo `assignsLeastLoadedAgentWhenSkillsMatch`.

## Seguridad y secretos

- Nunca leer, imprimir ni commitear `.env` ni claves.
- `TYPESAFE_API_KEY` se lee solo desde variables de entorno.
- No crear cuentas, no cargar medios de pago y no pegar credenciales en el chat. Eso lo hace el usuario.
- Antes de comandos destructivos (por ejemplo `docker compose down -v`), pedir confirmación.

## Git

- No commitear ni hacer push sin que lo pida el usuario.
- Mensajes de commit en inglés, con formato Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`).
- El cuerpo del commit explica el porqué, no repite el diff.

### Ramas

- `main`: solo estado verificado. Nunca se commitea directo.
- `develop`: rama de integración. Es la base de todo trabajo nuevo.
- Ramas de trabajo, siempre salidas de `develop`:
  - `feat/<descripcion-corta>` para funcionalidad nueva
  - `fix/<descripcion-corta>` para correcciones
  - `docs/<descripcion-corta>`, `chore/<descripcion-corta>`, `refactor/<descripcion-corta>`
- Flujo: `feat/x` → `develop` → `main`. A `main` se llega solo con `mvn verify` en verde.
- Los nombres de rama van en inglés y en kebab-case, por ejemplo `feat/decision-provider-port`.

### `main` está protegida

GitHub rechaza cualquier push directo a `main`, incluido el del dueño del repositorio. Para integrar:

```bash
gh pr create --base main --head develop --fill
```

La protección exige pull request, el check `build` en verde, y que la rama esté al día con `main`. No permite force push ni borrar la rama. Después de mergear, traer el resultado con `git switch main && git pull`.

No intentar `git push origin main`: siempre falla.

## Decisiones

- Cada decisión de arquitectura nueva va en un ADR en `docs/adr/`, numerado a partir de `ADR-002`.
- No cambiar el stack sin un ADR que lo justifique.
- Si una decisión cambia el plan, actualizar `docs/plan.md` en el mismo cambio.

## Qué no hacer

- No usar Gradle, Kotlin ni Jest en el backend.
- No agregar dependencias sin mencionarlo y justificarlo.
- No introducir Kafka, Redis ni multi-tenant. Están fuera del MVP.
- No implementar los módulos B (excepciones) ni C (inteligencia operativa) antes de cerrar el MVP.
- No inventar métricas en la documentación. Los números salen de la evaluación real con datos sintéticos.
- Si algo no está claro, preguntar antes de decidir arquitectura.

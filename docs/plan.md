# ops-engine: plan del MVP

Estado: planificación. No hay código todavía.

## 1. Alcance

**Dentro del MVP:** Módulo A (Smart Ticket Routing), con auditoría completa, revisión humana y evaluación offline.

**Próximas fases:** Módulo B (Exception Workflow) y Módulo C (Operations Intelligence).

**Dominio:** helpdesk de soporte técnico de una empresa de software B2B.

**Fuera del MVP:** multi-tenant (queda como ADR futuro), Kafka, despliegue en la nube y funciones avanzadas de frontend. El frontend mínimo sí entra.

## 2. Stack

| Área | Elección |
|---|---|
| Lenguaje | Java 21 LTS |
| Framework | Spring Boot (última versión estable) |
| Build | Maven |
| Modularidad | Paquetes por módulo, límites verificados con ArchUnit (ADR-002) |
| Arquitectura | Hexagonal en el núcleo de decisiones (puertos y adapters) |
| Persistencia | PostgreSQL, Spring Data JPA, Flyway para migraciones |
| Seguridad | Spring Security como resource server, JWT propio con HMAC (ADR-003) |
| Resiliencia | Resilience4j (timeout, retry, circuit breaker) |
| Eventos | Transactional outbox (tabla + `@Scheduled`); RabbitMQ solo si hace falta |
| Documentación API | springdoc-openapi |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers, WireMock, ArchUnit |
| Observabilidad | Actuator, Micrometer, Prometheus, Grafana, logs JSON, OpenTelemetry |
| IA | Puerto `DecisionProvider` con `FakeDecisionProvider` y `JevDecisionProvider` (ADR-005). Resiliencia con los módulos core de Resilience4j |
| Frontend | Next.js con TypeScript, Tailwind CSS y shadcn/ui; tipos generados desde OpenAPI; Jest + React Testing Library; Playwright para E2E |
| Infra local | Docker Compose; CI con GitHub Actions |

## 3. Modelo de dominio (borrador)

- **Ticket:** id, título, descripción, cliente, servicio, impacto, prioridad, estado, fecha límite de SLA, versión (optimistic locking), idempotency key.
- **Employee:** id, nombre, equipo, disponibilidad, capacidad máxima, versión.
- **Skill / EmployeeSkill:** competencias y nivel de cada empleado.
- **Permission:** qué tipos de incidente puede atender cada rol.
- **Team:** agrupa empleados.
- **Rule (versionada):** condiciones, umbrales y vigencia (desde / hasta).
- **Decision:** ticket, proveedor, entrada resumida, salida del proveedor, probabilidades, versión de reglas, acción ejecutada, motivo, latencia, fecha.
- **Assignment:** ticket, empleado, decisión asociada, fecha.
- **ReviewTask:** ticket, motivo de revisión, estado, resolución, motivo del operador si no está de acuerdo con la IA.
- **AuditEntry:** registro append-only de quién hizo qué y cuándo.
- **OutboxEvent:** tipo, payload, estado, intentos.

## 4. Estados del ticket

```
NUEVO → EVALUANDO → ASIGNADO → EN_PROGRESO → RESUELTO → CERRADO
                 ↘ REVISION_REQUERIDA → ASIGNADO (o CERRADO)
```

Las transiciones inválidas se rechazan en el dominio, no en el controlador.

## 5. Requisitos funcionales

- **RF-01** Crear ticket con idempotency key: la misma clave devuelve el mismo resultado y no duplica el ticket.
- **RF-02** Evaluar el ticket con `DecisionProvider` en una sola llamada, con preguntas de tipo choice, score y noul.
- **RF-03** Ruteo: buscar candidatos por skill y servicio, filtrar por disponibilidad, permisos y capacidad, y ordenar por carga, disponibilidad y SLA.
- **RF-04** Umbrales configurables y versionados: si la confianza supera el umbral se asigna; si no, el ticket pasa a `REVISION_REQUERIDA`.
- **RF-05** Combinar la IA con datos duros: el vencimiento real del SLA puede pesar más que la urgencia que expresa el texto. Esto se define como regla explícita.
- **RF-06** Asignación con control de concurrencia (optimistic locking) y evento en el outbox.
- **RF-07** Fallback: si el proveedor falla o hace timeout, el ticket pasa a `REVISION_REQUERIDA`. Nunca se pierde.
- **RF-08** Bandeja de revisión: el supervisor asigna o corrige, y registra el motivo cuando no está de acuerdo con la IA.
- **RF-09** Auditoría reproducible de cada decisión: versión de reglas, entrada resumida, salida del proveedor y acción.
- **RF-10** Evaluación offline: dataset etiquetado, con reporte de precisión, tasa de revisión e impacto en SLA.
- **RF-11** ABM de empleados, skills, permisos y reglas por API (rol ADMIN).
- **RF-12** Autenticación y roles: AGENTE, SUPERVISOR, ADMIN.

## 6. Requisitos no funcionales

- **RNF-01 Latencia:** objetivo a validar, por ejemplo p95 menor a 300 ms en creación de ticket sin contar la llamada a la IA.
- **RNF-02 Tolerancia a fallos:** la caída del proveedor de IA no tira el sistema.
- **RNF-03 Trazabilidad:** toda decisión se puede reproducir a partir de la auditoría.
- **RNF-04 Privacidad:** no se guardan datos personales innecesarios y no se loguea el contenido completo de los tickets.
- **RNF-05 Seguridad:** los secretos (por ejemplo `TYPESAFE_API_KEY`) van por variables de entorno del sistema o del CI. Nunca en el repositorio, en los logs, en el chat ni en archivos de configuración versionados.
- **RNF-06 Pruebas:** cada regla de ruteo tiene tests parametrizados, incluidas las ramas de error.

## 7. Estrategia de tests

- **Unitarios:** reglas de ruteo, máquina de estados, cálculo de SLA.
- **Integración:** API + PostgreSQL con Testcontainers. Flujo completo: crear, evaluar, asignar.
- **Contrato del proveedor:** WireMock simula Jev con respuesta válida, timeout, error 500 y respuesta malformada.
- **Concurrencia:** dos hilos asignando el mismo ticket. Uno gana y el otro recibe conflicto.
- **Arquitectura:** ArchUnit y los tests de Spring Modulith.
- **Evaluación offline:** no es un test unitario. Es un comando que genera un reporte.

## 8. Fases

| Fase | Contenido |
|---|---|
| 0. Base | Repositorio, Maven, Docker Compose con PostgreSQL, CI, ADR-001, README |
| 1. Dominio | Ticket, Employee, Skills, máquina de estados, tests unitarios |
| 2. Persistencia y API | Flyway, JPA, endpoints, OpenAPI, idempotencia, autenticación. Logs JSON y health desde acá |
| 2.1 Pendiente de fase 2 | Clientes, servicios y prioridad del ticket: llegan en la fase 4 con el ruteo, en su propia migración |
| 3. IA | `DecisionProvider`, Fake, adapter de Jev con WireMock, Resilience4j, fallback |
| 4. Ruteo y asignación | Reglas, umbrales, optimistic locking, outbox |
| 5. Revisión humana y auditoría | Bandeja de revisión, motivo de desacuerdo, auditoría completa |
| 6. Evaluación offline | Dataset de 30 a 50 tickets, reporte, tasa de acuerdo IA-humano |
| 7. Observabilidad | Dashboards y alertas en Grafana (ver `monitoring.md`) |
| 8. Frontend | Next.js: lista de tickets, detalle con decisión, bandeja de revisión y panel de métricas |
| 9. Próximas fases | Módulos B y C |

## 9. ADRs previstos

**Escritos**
- ADR-001: Spring Boot con Java 21 y Maven
- ADR-002: Módulos por paquete verificados con ArchUnit, sin Spring Modulith
- ADR-003: Autenticación con JWT propio y resource server
- ADR-004: La aplicación corre en UTC
- ADR-005: `DecisionProvider` como puerto, y la IA nunca decide

**Previstos**
- ADR-006: Outbox en lugar de un broker
- ADR-007: Optimistic locking para asignaciones
- ADR-008: Resiliencia y fallback a revisión
- ADR-009: Estrategia de evaluación offline

## 10. Riesgos

- **Acceso a Jev no confirmado.** Mitigación: el `FakeDecisionProvider` permite avanzar sin la API, y el adapter es intercambiable.
- **Alcance excesivo.** Mitigación: Módulos B y C quedan fuera del MVP.
- **Calidad de las decisiones de la IA.** Mitigación: evaluación offline y tasa de override como métrica.
- **Costo de las llamadas.** Mitigación: medir cantidad de requests y tokens desde el inicio. Confirmar precios con TypeSafe.

## 11. Decisiones y pendientes

**Decididas**
- Build: Maven.
- Dominio: helpdesk de soporte técnico de una empresa de software B2B.
- Frontend: incluido en el MVP (ver sección 2).
- Multi-tenant: no en el MVP. Queda como ADR futuro.

**Pendientes**
- **Acceso a Jev:** conseguir la clave en console.typesafe.ai/keys y revisar la facturación en la consola. Las docs oficiales no detallan cómo se carga el pago ni si hay créditos de prueba.
- **Guardado de la clave:** variable de entorno del usuario en el equipo de desarrollo, y secreto de GitHub Actions en el CI.

## 12. Datos de demo (simulación)

No hay usuarios reales, así que el sistema se alimenta con datos sintéticos en tres capas:

**a) Datos semilla**, para que la demo funcione desde el primer arranque:
- 4 equipos: Accesos e identidad, Facturación, Integraciones e Infraestructura.
- 24 agentes distribuidos en los equipos, con 1 a 3 skills cada uno, disponibilidad y capacidad máxima.
- 3 supervisores y 1 administrador.
- 20 clientes ficticios con un plan (básico, pro o enterprise), que define el SLA.
- 10 servicios o productos, por ejemplo API de pagos, portal web, app móvil y sincronización de catálogo.
- Reglas iniciales (versión 1) y umbrales.
- Usuarios de prueba para cada rol, solo en el perfil `dev`, con contraseñas de desarrollo. Nunca en producción.

**b) Dataset de evaluación:** 30 a 50 tickets con el resultado esperado (equipo y prioridad). Los etiqueta una persona con criterio de soporte real. No se etiquetan con las mismas reglas del sistema, porque la evaluación se volvería circular.

**c) Generador de carga:** de 1.000 a 5.000 tickets sintéticos, creados desde plantillas de texto con variaciones de cliente, servicio, impacto y fecha, repartidos en la última semana. Sirve para probar rendimiento, cola sin asignar, SLA en riesgo y dashboards. Se ejecuta con un comando.

**Volumen:** para el portfolio no hacen falta miles de tickets reales. El generador sirve para carga y dashboards; 30 a 50 tickets bien etiquetados valen más para la evaluación.

**Costo:** la carga se corre con `FakeDecisionProvider`, así no se gasta presupuesto ni se pegan los límites de tasa de Jev. Jev se usa solo para el dataset de evaluación y para una demo puntual. Con el precio publicado en la documentación (USD 0,042 por millón de tokens de entrada), 5.000 decisiones de 2.000 tokens cada una cuestan alrededor de USD 0,42.

## 13. Resultados a mostrar (en orden de prioridad)

1. **Explicación de cada decisión (lo más importante).** En el detalle del ticket: equipo y agente asignados, candidatos descartados con el motivo (sin skill, sin disponibilidad, sin permiso o capacidad llena), probabilidades de la IA, regla y versión aplicadas, y si cayó en revisión y por qué. Es lo que muestra que el sistema toma decisiones explicables y no es una caja negra.
2. **Comparación contra baseline (el número que vende).** Sobre el mismo dataset, tres estrategias: (a) round-robin o asignación por carga, (b) solo reglas, (c) reglas + IA. Métricas: exactitud de equipo, tasa de revisión y, sobre todo, **errores silenciosos**, que son asignaciones incorrectas que no pasaron a revisión. Si reglas + IA reduce los errores silenciosos frente a solo reglas, ahí está el valor de la IA. Si no lo reduce, igual es un resultado honesto y se muestra.
3. **Impacto simulado en SLA:** con los tickets generados, cumplimiento de SLA con y sin ruteo. Debe quedar marcado como simulación.
4. **Fallback probado:** forzar timeouts o apagar el proveedor y mostrar que los tickets pasan a revisión y ninguno se pierde.
5. **Auditoría reproducible:** tomar una decisión antigua y reconstruirla con la versión de reglas guardada.
6. **Dashboard de Grafana** con carga: cola sin asignar, SLA en riesgo, latencia p95 y tasa de override.
7. **Test de concurrencia:** dos asignaciones simultáneas no se pisan.

Si solo se puede mostrar dos cosas, son la 1 y la 2.

**Qué evitar:** un número de precisión sin dataset etiquetado ni método. Un reclutador técnico lo cuestiona enseguida. El README debe decir que los datos son sintéticos.

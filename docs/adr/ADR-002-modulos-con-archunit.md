# ADR-002: Módulos por paquete verificados con ArchUnit, sin Spring Modulith

- **Estado:** Aceptada
- **Fecha:** 2026-11-06

## Contexto

El plan original (`docs/plan.md`, sección 2) proponía Spring Modulith para separar los módulos de negocio y verificar sus límites con tests.

Al implementar las fases 1 y 2 quedó claro que lo que el proyecto necesita es una regla simple y explícita: el dominio no depende de frameworks, los módulos no se tocan entre sí por dentro y las dependencias apuntan hacia adentro. ArchUnit ya estaba en el proyecto desde la fase 1 para la primera de esas reglas.

## Decisión

- Un paquete por módulo de negocio: `ticket`, `employee`, `team`, más `shared` como núcleo común.
- Dentro de cada módulo, tres capas: `domain`, `application` y `adapter`.
- Los límites se verifican con `DomainArchitectureTest` (ArchUnit), que falla el build si:
  - el dominio importa Spring, JPA o Swagger;
  - el dominio depende de `application` o `adapter`;
  - `application` depende de `adapter`;
  - `ticket` y `employee` se referencian entre sí;
  - `shared` depende de algún módulo;
  - una entidad JPA aparece fuera de un adapter de persistencia.
- Lo transversal de la capa web (`GlobalExceptionHandler`, `ApiError`) vive en `api`, no en `shared`, para que `shared` no dependa de los módulos.
- No se agrega Spring Modulith.

## Consecuencias

**Positivas**
- Una dependencia menos y ningún runtime extra que entender.
- Las reglas están escritas en un solo archivo y se leen como prosa.
- ArchUnit ya era parte del stack, así que no hay nada nuevo que aprender.

**Negativas**
- Se pierden las funciones de Spring Modulith que no estamos usando: documentación automática de módulos, eventos entre módulos y tests de integración por módulo.
- Las reglas hay que escribirlas a mano. Un módulo nuevo exige acordarse de agregar su regla.
- Si en la fase 4 los módulos necesitan comunicarse por eventos, conviene reconsiderar esta decisión en un ADR nuevo.

## Alternativas consideradas

- **Spring Modulith:** da más por defecto (documentación, eventos, verificación automática), pero suma una dependencia y un modelo propio para un proyecto con tres módulos.
- **Módulos Maven separados:** aislamiento real en tiempo de compilación, a costa de un build multi-módulo y mucha más ceremonia para el tamaño del proyecto.

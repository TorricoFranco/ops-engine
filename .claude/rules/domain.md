---
paths:
  - "src/main/java/**/domain/**"
---

# Reglas del dominio

- Java puro: sin imports de Spring, JPA, Jackson ni de los adapters. `DomainArchitectureTest` lo verifica.
- Las reglas viven en los aggregates (`Ticket`, `Employee`), no en servicios ni en controladores.
- El estado del ticket cambia solo con los métodos del aggregate. Nunca exponer un setter de `status`.
- Los invariantes se validan en la creación y en cada método que cambia estado.
- Errores: `IllegalArgumentException` para datos inválidos, `InvalidTicketTransitionException` para transiciones prohibidas, `IllegalStateException` para capacidad o disponibilidad del empleado.
- Cada cambio de comportamiento tiene su test unitario en `src/test`, y `DomainArchitectureTest` tiene que seguir en verde.

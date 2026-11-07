---
name: add-feature
description: Agrega un caso de uso o una regla nueva a un módulo del proyecto, con sus tests y siguiendo las reglas de CLAUDE.md. Usar al implementar un requisito funcional (RF-xx) de docs/plan.md.
---

# Agregar una funcionalidad

1. Confirmar el requisito en `docs/plan.md`, sección 5 (RF-xx). Si no existe, preguntar antes de implementar.
2. Empezar por el dominio:
   - Cambiar o agregar métodos en el aggregate correspondiente, con sus invariantes.
   - Escribir el test unitario en `src/test/java` junto con el cambio. Seguir `.claude/rules/tests.md`.
3. Caso de uso en `application`: una clase que orqueste el dominio y dependa de interfaces (puertos), nunca de implementaciones.
4. Adapter en `adapter`:
   - Controlador REST fino, con DTOs como `record`.
   - Las excepciones de dominio se mapean en el único `@RestControllerAdvice`.
   - Desde la fase 2: tests de integración con Testcontainers para la persistencia y el HTTP.
5. Si la funcionalidad toca el ruteo, la IA o la auditoría, revisar que se registre lo que pide `docs/plan.md`, sección 13.
6. Correr `mvn verify` y dejarlo en verde antes de dar la tarea por terminada. Mientras Maven no esté en el PATH, usar `C:\Users\Franco\tools\apache-maven-3.9.16\bin\mvn`.
7. Si cambió la fase o el estado del proyecto, actualizar la sección "Estado" de `CLAUDE.md`.
8. No commitear sin que el usuario lo pida.

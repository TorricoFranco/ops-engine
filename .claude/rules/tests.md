---
paths:
  - "src/test/java/**"
---

# Reglas de tests

- JUnit 5 y AssertJ. No usar Hamcrest ni `assertEquals` de JUnit.
- Las reglas con varios casos van en `@ParameterizedTest`, con `@CsvSource`, `@EnumSource` o `@MethodSource`.
- Los tests de dominio no levantan Spring: se instancian los objetos directamente.
- El tiempo de los tests es fijo (un `Instant` constante). Nunca `Instant.now()`.
- Los nombres de los métodos están en inglés y describen el comportamiento, por ejemplo `cannotAssignDirectlyFromNuevo`.

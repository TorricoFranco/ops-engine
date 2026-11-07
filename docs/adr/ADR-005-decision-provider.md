# ADR-005: `DecisionProvider` como puerto, y la IA nunca decide

- **Estado:** Aceptada
- **Fecha:** 2026-11-07

## Contexto

El motor necesita juicios sobre el texto de cada ticket: qué competencia hace falta, cuánto le impide operar al cliente, si hay varios afectados. Esos juicios los da Jev, de TypeSafe.

Hay dos riesgos. El primero es de acoplamiento: si las reglas de negocio hablan directamente con el SDK o con el JSON del proveedor, cambiar de proveedor obliga a tocar el dominio. El segundo es de diseño: es tentador preguntarle al modelo directamente "¿hay que escalar este ticket?", y esa pregunta depende de permisos, del SLA y de la carga del equipo, que son datos que el modelo no tiene y que la aplicación sí.

La documentación de TypeSafe es explícita: el modelo aporta los juicios y el código decide qué hacer con ellos.

## Decisión

- Un puerto `DecisionProvider` en `decision/application`, con dos implementaciones: `FakeDecisionProvider` y `JevDecisionProvider`. Se elige con `app.decision.provider`, y el valor por defecto es `fake`, así el sistema arranca sin salir a la red.
- **Las preguntas piden juicios observables, no decisiones.** No existe un "¿hay que escalar esto?". Se pregunta qué competencia hace falta, cuánto le impide operar al cliente, si el texto menciona varios afectados y si menciona un servicio caído. El ruteo (fase 4) combina esas respuestas con los datos duros del sistema.
- **El contexto no incluye datos duros.** `TicketContext` manda solo el título y la descripción. El impacto declarado y el vencimiento del SLA quedan afuera: el sistema ya los conoce, y mandarlos sesgaría el juicio de urgencia hacia lo que escribió quien abrió el ticket.
- **Un choice siempre elige una opción, aunque ninguna encaje.** Por eso va acompañado de un noul, `skill_recognized`, que pregunta si el ticket corresponde a alguna de las competencias. Sin esa pregunta, un ticket de otra área recibiría igual una competencia asignada.
- **Las respuestas son tipos del dominio, no booleanos.** `NoulAnswer` es una probabilidad: un valor cerca de 0,5 significa que el contexto no resuelve la pregunta, y ese caso tiene su propia rama. `ScoreAnswer` conserva la distribución porque su valor es un promedio y puede ocultar dos picos. `ChoiceAnswer` conserva las probabilidades de todas las opciones.
- **Todo el texto que ve el modelo vive en `JevQuestions`.** Es lo que más se audita y se ajusta, así que no se mezcla con el código de transporte.
- **Una falla nunca se convierte en un juicio.** Cualquier error termina en `DecisionUnavailableException`, y el ruteo manda el ticket a revisión (RF-07). Tratar una llamada fallida como un "no" haría que el sistema tomara ruido por señal.
- **Resiliencia armada a mano**, con los módulos core de Resilience4j en lugar de la integración por AOP: timeouts en el cliente HTTP, reintentos solo para fallas de transporte, y un circuit breaker que corta las llamadas cuando el proveedor viene fallando. Una respuesta que llegó pero no se entiende no se reintenta: reintentar no la va a cambiar.

## Consecuencias

**Positivas**
- El dominio no conoce a TypeSafe. Cambiar de proveedor es escribir otro adapter.
- Se puede desarrollar, demostrar y testear sin clave y sin presupuesto.
- El `FakeDecisionProvider` da una línea de base contra la cual comparar en la evaluación offline (fase 6): si las reglas más el modelo no le ganan a un contador de palabras clave, eso también es un resultado.
- Los tests de contrato con WireMock cubren respuesta válida, error, timeout y respuesta mal formada sin tocar la red.

**Negativas**
- Hay un mapeo a mano entre el JSON del proveedor y los tipos del dominio. Si TypeSafe cambia el formato, hay que tocarlo, y los tests con WireMock no lo detectarían porque simulan el formato viejo.
- Cinco preguntas en un pedido comparten el estado, pero también significan que cambiar una obliga a revisar la respuesta completa.
- Los umbrales de 0,7 y 0,3 son los que recomienda la documentación, no valores medidos sobre estos datos. Hay que ajustarlos con el dataset de evaluación.
- Armar la resiliencia a mano es más código que anotar métodos, a cambio de poder probarla sin levantar Spring.

## Alternativas consideradas

- **Llamar al SDK desde el caso de uso:** menos código, pero ata las reglas de negocio al proveedor y hace imposible testear sin red.
- **Preguntarle al modelo la decisión final:** una sola pregunta en lugar de cinco, pero el modelo no conoce los permisos, el SLA real ni la carga del equipo, y la decisión dejaría de ser auditable.
- **`resilience4j-spring-boot4` con anotaciones:** más declarativo, pero mete AOP y autoconfiguración en el camino y obliga a levantar un contexto de Spring para probar el comportamiento de reintentos.
- **Devolver un juicio neutro cuando el proveedor falla:** evita la excepción, pero convierte una falla en un dato y rompe la trazabilidad.

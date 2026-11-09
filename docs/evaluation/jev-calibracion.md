# Primera calibración contra la API real de Jev

Fecha: 2026-11-09. Modelo `jev-latest`. Tres casos, no es una evaluación.

Esto **no** reemplaza la evaluación offline: son tres tickets inventados que sirvieron para
verificar que el formato programado coincide con el real y para ajustar la redacción de una
pregunta. Los números de verdad salen del dataset etiquetado.

## Qué se encontró

La pregunta que acompaña al choice, la que detecta que un ticket no corresponde a ninguna
competencia, **no estaba discriminando**. Con la redacción original:

| Caso | `skill_recognized` |
|---|---|
| Cobro duplicado (caso claro) | 0,83 |
| Consulta de horarios (fuera de alcance) | 0,72 |

Los dos por encima del umbral de 0,7. Visto en frío tiene sentido: una consulta de horarios
sigue siendo algo que un cliente le escribe a soporte, y la pregunta decía justamente eso.

Lo que salvaba el caso era el otro umbral, el de confianza del choice: la consulta repartía la
probabilidad entre las cuatro opciones (0,47 / 0,23 / 0,18 / 0,12) y no llegaba a 0,6. Es decir,
el sistema acertaba por una razón distinta de la que estaba diseñada.

## El primer intento se pasó de largo

Reformular a *"¿describe un problema técnico con el producto?"* resolvió el falso positivo pero
rompió el caso bueno: un cobro duplicado bajó a 0,12. Y está bien que así sea, porque un cobro
mal hecho no es una falla del producto. La pregunta había quedado más angosta que el dominio.

## La redacción que quedó

La distinción no es técnico contra no técnico, sino **tener un problema concreto con el
servicio contratado** frente a **consultar algo sobre la empresa**.

| Caso | `skill_recognized` | Competencia elegida |
|---|---|---|
| Cobro duplicado | **0,97** | BILLING (p=1,00) |
| Quiero contratar un plan más grande | **0,06** | BILLING (p=1,00) |
| Consulta de horarios | **0,04** | INTEGRATIONS (p=0,56) |

El caso comercial es el que mejor muestra para qué sirve esta pregunta: el choice elige BILLING
con probabilidad 1,00, porque de las cuatro áreas es la que más se parece a una consulta sobre
planes. Si solo se mirara el choice, el ticket se asignaría a facturación con total confianza.
La pregunta acompañante lo baja a 0,06 y el ruteo lo manda a revisión, que es lo correcto.

## Detalles para tener en cuenta

- **Jev no es determinista.** Dos llamadas idénticas dieron 0,83 y 0,82, y urgencias de 0,97 y
  1,03. Por eso los tests afirman la forma de la respuesta y comparaciones relativas, nunca un
  valor exacto, y por eso dos corridas de la evaluación pueden diferir un poco.
- **La latencia observada fue de 350 a 560 ms** por llamada, con las cinco preguntas juntas.
- **La redacción se ajustó dos veces mirando resultados.** El caso comercial se agregó como
  caso reservado, sin usarlo para calibrar, y la versión final lo resuelve bien. Aun así, tres
  casos son pocos: la validación de verdad es el dataset etiquetado.

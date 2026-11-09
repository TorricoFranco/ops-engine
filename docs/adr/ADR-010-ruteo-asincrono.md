# ADR-010: El ruteo corre en segundo plano, fuera de la transacción

- **Estado:** Aceptada
- **Fecha:** 2026-11-09

## Contexto

Hasta la fase 5, rutear un ticket era una llamada HTTP que esperaba al proveedor de juicios, y todo eso ocurría dentro de una transacción de base de datos.

Las mediciones contra la API real dieron entre 350 y 560 ms por llamada, con un timeout configurado en 10 segundos. Durante todo ese tiempo la transacción mantenía tomada una conexión del pool. La base no estaba trabajando: estaba esperando a un tercero.

Con poco volumen no se nota. Con miles de tickets por día, el pool se agota mientras la base está ociosa, y el sistema se cae por una espera que no le aporta nada a quien creó el ticket.

## Decisión

### La transacción se abre después del juicio

`RouteTicketUseCase.route` ya no está anotado como transaccional. Primero lee el ticket, después consulta al proveedor **sin transacción**, y recién con la respuesta en la mano abre una para escribir.

La transacción se maneja con `TransactionTemplate` y no con una anotación. Es a propósito: anotar un método privado, o uno llamado desde la misma clase, no hace nada, porque el proxy de Spring no se entera. El template, además, deja el límite visible justo donde importa.

### El ticket se vuelve a leer dentro de la transacción

Entre la lectura inicial y la escritura pasó una llamada a la red. Trabajar sobre la copia vieja sería escribir sobre un estado que puede haber cambiado.

### Un worker en segundo plano

`RoutingWorker` toma los tickets en `NUEVO` de a lotes, los más viejos primero, y los rutea. Crear un ticket responde al instante.

El endpoint `POST /api/tickets/{id}/routing` sigue existiendo, porque para la demo conviene poder disparar el ruteo de un ticket puntual y ver la respuesta con su explicación.

### Nada queda trabado

El ticket pasa a `EVALUANDO` y de ahí al estado final **dentro de la misma transacción**. Si el proceso se cae durante la llamada al proveedor, el ticket sigue en `NUEVO` y la próxima pasada lo toma. No hace falta un proceso que rescate tickets colgados, porque no hay un estado intermedio persistido donde puedan colgarse.

Si dos intentos coinciden sobre el mismo ticket, el optimistic locking hace fallar al segundo y el worker lo descarta en silencio: el ticket ya quedó resuelto.

## Consecuencias

**Positivas**
- Ninguna conexión de base queda esperando a un servicio externo.
- Crear un ticket no depende de que el proveedor esté disponible ni sea rápido.
- Un pico de tickets se absorbe en la cola en lugar de rebotar.
- Un fallo transitorio se reintenta solo en la pasada siguiente.

**Negativas**
- **Con varias instancias corriendo, dos workers pueden juzgar el mismo ticket a la vez.** Solo una escritura prospera, pero la llamada al proveedor se paga dos veces. Con una instancia no pasa. Resolverlo requiere tomar el ticket con `SELECT ... FOR UPDATE SKIP LOCKED`, y queda pendiente para cuando haya más de una instancia.
- **Un ticket que falla siempre se reintenta para siempre.** No hay tope de intentos ni cola de descarte. Se ve en el log y en la cantidad de pendientes, pero debería tener un límite.
- El ruteo ya no es inmediato: hay hasta dos segundos de demora. Para este caso de uso es irrelevante, pero deja de ser cierto que al responder el POST el ticket ya esté asignado.
- Hay dos caminos para rutear, el worker y el endpoint. Es deliberado para la demo, y significa que un cambio en el flujo hay que pensarlo en los dos.

## Alternativas consideradas

- **Dejarlo síncrono y agrandar el pool:** corre el problema más lejos sin resolverlo, y empeora el consumo de memoria de la base.
- **Rutear al crear el ticket, en la misma llamada:** la peor opción, porque le traslada al cliente la latencia y la disponibilidad del proveedor.
- **Una cola real, como RabbitMQ:** es la respuesta correcta con varias instancias y mayor volumen, pero sumarla ahora agrega un servicio al despliegue para un problema que una consulta por estado resuelve. El outbox ya está, y migrar a un broker después es cambiar quién alimenta al worker.
- **Tomar el ticket marcándolo `EVALUANDO` y luego juzgar:** permitiría varias instancias sin trabajo duplicado, pero crea un estado del que un proceso caído no vuelve, y obliga a un rescatador con sus propios tiempos de espera y topes de reintento. No vale la pena mientras haya una sola instancia.

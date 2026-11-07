# ADR-004: La aplicación corre en UTC

- **Estado:** Aceptada
- **Fecha:** 2026-11-06

## Contexto

`CLAUDE.md` ya fijaba que en persistencia se guardan `Instant` en UTC y que la zona horaria se usa solo al presentar. Al levantar los primeros tests de integración con Testcontainers apareció un problema concreto: el driver de PostgreSQL le informa a la conexión la zona por defecto de la JVM, y la máquina de desarrollo está en `America/Buenos_Aires`. La imagen `postgres:17` rechaza ese identificador:

```
FATAL: invalid value for parameter "TimeZone": "America/Buenos_Aires"
```

El contexto de Spring no levantaba y los 16 tests de integración fallaban. El mismo problema aparecería en cualquier máquina cuya zona no coincida con las que conoce el contenedor.

## Decisión

- La JVM corre en UTC. `OpsEngineApplication.main` hace `TimeZone.setDefault(UTC)` antes de arrancar Spring.
- Los tests corren igual: surefire pasa `-Duser.timezone=UTC`.
- Se mantiene `hibernate.jdbc.time_zone: UTC`.
- La zona del usuario se aplica solo al presentar y en el calendario de SLA (fase 4), nunca en el almacenamiento.

## Consecuencias

**Positivas**
- El comportamiento no depende de la zona de la máquina: lo mismo en una laptop, en CI y en un servidor.
- Los timestamps de los logs quedan en UTC, que es lo que conviene para correlacionar entre servicios.
- Desaparece la clase de bugs de cambio de horario en los cálculos de SLA.

**Negativas**
- Los logs locales no se leen en hora argentina. Hay que tenerlo presente al depurar.
- `TimeZone.setDefault` es estado global del proceso: afecta a toda la JVM, no solo a esta aplicación.
- Cualquier salida pensada para una persona tiene que convertir la zona de forma explícita. Es un paso que se puede olvidar.

## Alternativas consideradas

- **Forzar la zona solo en la conexión JDBC** (`options=-c TimeZone=UTC`): arregla el síntoma en la base, pero deja el resto de la JVM en la zona local, así que los cálculos en memoria siguen dependiendo de la máquina.
- **Usar el identificador completo `America/Argentina/Buenos_Aires`:** el contenedor lo aceptaría, pero el comportamiento seguiría atado a la zona de quien corre el proyecto.
- **No tocar nada y documentar el requisito:** deja el build roto en cualquier máquina con una zona que el contenedor no reconozca.

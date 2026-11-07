# ADR-003: Autenticación con JWT propio y resource server

- **Estado:** Aceptada
- **Fecha:** 2026-11-06

## Contexto

RF-12 pide autenticación y tres roles: AGENT, SUPERVISOR y ADMIN. La API no tiene estado y la va a consumir un frontend en Next.js (fase 8), así que una sesión con cookie no encaja bien. Tampoco hay un proveedor de identidad externo, y montar uno para un proyecto de portfolio agrega infraestructura sin aportar al tema central, que es el motor de decisiones.

## Decisión

- La aplicación emite sus propios JWT firmados con HMAC (HS256) en `POST /api/auth/login`.
- Los valida Spring Security como OAuth2 Resource Server, con `NimbusJwtDecoder`. Es el mismo camino que se usaría con un proveedor externo, así que cambiar a uno después es reemplazar el decoder.
- El rol viaja en el claim `roles` y `JwtGrantedAuthoritiesConverter` lo convierte en `ROLE_*`. La autorización por endpoint usa `@PreAuthorize`.
- Las sesiones son `STATELESS` y CSRF queda desactivado, porque no hay cookies de sesión.
- Las contraseñas se guardan con BCrypt en la tabla `app_users`.
- La clave de firma sale de `app.security.jwt.secret`. En desarrollo tiene un valor por defecto; en cualquier otro entorno viene de la variable de entorno `JWT_SECRET`. `JwtProperties` falla el arranque si la clave tiene menos de 32 bytes.
- Los usuarios de prueba los crea `DevUserSeeder`, solo con el perfil `dev`, hasheando la contraseña con el `PasswordEncoder` de la aplicación. No hay hashes fijos en las migraciones.
- El login responde lo mismo ante un usuario inexistente y ante una contraseña incorrecta, para no revelar qué usuarios existen.

## Consecuencias

**Positivas**
- Sin infraestructura extra: no hace falta Keycloak ni un proveedor en la nube.
- El camino de validación es el estándar de Spring Security, así que migrar a OIDC después es acotado.
- El secreto nunca está en el repositorio y el arranque falla temprano si falta o es débil.

**Negativas**
- Un JWT emitido no se puede revocar antes de que venza. Con un TTL de 8 horas, un token robado sirve hasta ese plazo. Si hiciera falta revocación, habría que agregar refresh tokens y una lista de revocación.
- HMAC usa un secreto compartido: cualquier servicio que valide el token puede también emitirlo. Con más de un servicio convendría pasar a RSA o a un proveedor externo.
- Emitir tokens es responsabilidad de la aplicación, que es justo lo que delegaría un sistema en producción.

## Alternativas consideradas

- **Proveedor externo (Keycloak, Auth0):** más realista para una empresa, pero agrega un servicio al Docker Compose y desvía el foco del proyecto.
- **HTTP Basic:** más simple, pero manda las credenciales en cada pedido y no encaja con un frontend SPA.
- **Sesión con cookie:** válida para un monolito con vistas del servidor; no para una API consumida por Next.js.

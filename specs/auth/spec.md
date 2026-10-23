---
title: Auth — Especificación funcional (H2)
status: aprobada
source: https://app.notion.com/p/3ab75bb08ca781a1bdfbca062c033586
updated: 2026-10-23T20:58:23.842Z
---

# Auth — Especificación funcional (H2)

## Propósito

Construir el servicio de autenticación de Meridian (`services/auth-service`) con registro
persistente en MongoDB, login, sesión JWT, consulta de perfil y logout. Los usuarios
registrados DEBEN conservarse entre reinicios y poder iniciar sesión desde web o mobile.

Esta especificación cubre el contrato funcional completo del área auth. Los detalles de
cada hito de implementación se encuentran en las sub-specs:
- Persistencia: `auth/persistencia` (H2.1)
- Registro + hashing: `auth/registro-seguro` (H2.2)
- Tests de integración: `auth/tests-auth-mongodb` (H2.3)
- Infra local: `infra/mongodb-local` (H2.4)

## Requisitos

### Requisito: Registro de usuario

El sistema DEBE exponer el endpoint `POST /api/v1/auth/register` para registrar nuevos
usuarios, validar sus datos, persistir el documento en MongoDB y emitir un JWT en la
respuesta. Ver detalles en `auth/registro-seguro`.

#### Escenario: Registro exitoso

- **DADO** que un cliente envía `name` no vacío, `email` con formato válido y `password`
  de mínimo 8 caracteres, y el email no está registrado
- **CUANDO** se invoca `POST /api/v1/auth/register`
- **ENTONCES** el sistema responde HTTP 201 Created
- **Y** el cuerpo contiene `accessToken` (JWT válido), `tokenType: "Bearer"`,
  `expiresIn` (segundos) y `user` con `id`, `name`, `email`
- **Y** `passwordHash` NO aparece en ningún campo de la respuesta

#### Escenario: Email ya registrado

- **DADO** que ya existe un usuario con el mismo email normalizado
- **CUANDO** se invoca `POST /api/v1/auth/register`
- **ENTONCES** el sistema responde HTTP 409 Conflict
- **Y** el segundo usuario NO queda persistido

#### Escenario: Datos inválidos

- **DADO** que el payload omite un campo obligatorio o el password tiene menos de 8 caracteres
- **CUANDO** se invoca `POST /api/v1/auth/register`
- **ENTONCES** el sistema responde HTTP 400 Bad Request
- **Y** la respuesta sigue el contrato de error estándar del servicio

---

### Requisito: Login de usuario

El sistema DEBE exponer el endpoint `POST /api/v1/auth/login` para autenticar usuarios
ya registrados mediante email y password. DEBE buscar el usuario por email normalizado
en MongoDB y validar el password contra el hash almacenado.

#### Escenario: Login exitoso

- **DADO** que existe un usuario registrado con email `ada@example.com` y password `DemoPass123!`
- **CUANDO** se invoca `POST /api/v1/auth/login` con esas credenciales
- **ENTONCES** el sistema responde HTTP 200 OK
- **Y** el cuerpo contiene el mismo contrato de sesión que register:
  `accessToken`, `tokenType`, `expiresIn` y `user` con `id`, `name`, `email`
- **Y** `passwordHash` NO aparece en la respuesta

#### Escenario: Password incorrecto

- **DADO** que existe un usuario con email `ada@example.com`
- **CUANDO** se invoca `POST /api/v1/auth/login` con password incorrecto
- **ENTONCES** el sistema responde HTTP 401 Unauthorized con mensaje genérico
- **Y** la respuesta NO indica si el email existe o no (evitar enumeración de usuarios)

#### Escenario: Email inexistente

- **DADO** que no existe ningún usuario con el email provisto
- **CUANDO** se invoca `POST /api/v1/auth/login`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized con mensaje genérico

#### Escenario: Login solo desde MongoDB

- **DADO** que el servicio arranca sin seed de in-memory
- **CUANDO** se intenta login con credenciales no presentes en MongoDB
- **ENTONCES** el sistema responde HTTP 401 (no usa fallback in-memory ni datos fijos en código)

---

### Requisito: Consulta de perfil autenticado (`/me`)

El sistema DEBE exponer el endpoint `GET /api/v1/auth/me` para devolver el perfil del
usuario autenticado, leyendo el documento desde MongoDB mediante el identificador
contenido en el JWT.

#### Escenario: Perfil leído desde MongoDB

- **DADO** que el usuario está autenticado y envía el header
  `Authorization: Bearer <token válido>`
- **CUANDO** se invoca `GET /api/v1/auth/me`
- **ENTONCES** el sistema responde HTTP 200 OK
- **Y** el cuerpo contiene el perfil con al menos `id`, `name`, `email`
- **Y** `passwordHash` NO aparece en la respuesta

#### Escenario: Token inválido o expirado

- **DADO** que el cliente envía un token JWT inválido, expirado o ausente
- **CUANDO** se invoca `GET /api/v1/auth/me`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

---

### Requisito: Logout

El sistema DEBE exponer el endpoint `POST /api/v1/auth/logout` para invalidar la sesión
del usuario. En la implementación inicial (JWT sin revocación de servidor), el logout es
del lado del cliente; el servidor DEBE devolver 204 No Content.

#### Escenario: Logout exitoso

- **DADO** que el cliente envía una solicitud de logout (con o sin token)
- **CUANDO** se invoca `POST /api/v1/auth/logout`
- **ENTONCES** el sistema responde HTTP 204 No Content

---

### Requisito: OpenAPI publicado

El sistema DEBE exponer la documentación OpenAPI del `auth-service` con al menos los
cuatro endpoints del área auth: register, login, me, logout.

#### Escenario: OpenAPI accesible en el servicio

- **DADO** que el `auth-service` está corriendo
- **CUANDO** se accede a la URL de OpenAPI (ej. `/v3/api-docs` o `/swagger-ui`)
- **ENTONCES** el sistema devuelve la especificación OpenAPI con los endpoints
  `POST /api/v1/auth/register`, `POST /api/v1/auth/login`,
  `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` documentados

---

### Requisito: Seed de usuario opcional e idempotente

El sistema DEBE ofrecer un mecanismo de carga inicial de datos (seed) para crear un
usuario de demo en MongoDB al arrancar. El seed DEBE ser idempotente: ejecutarlo más
de una vez (reinicios del servicio) NO DEBE duplicar el usuario ni generar errores.

#### Escenario: Seed crea usuario de demo en arranque inicial

- **DADO** que el servicio arranca sobre una base MongoDB vacía y el seed está activado
- **CUANDO** el servicio completa su inicialización
- **ENTONCES** existe al menos un usuario de demo en la colección `users`
- **Y** el password del usuario de demo está almacenado como hash BCrypt

#### Escenario: Seed idempotente en reinicio

- **DADO** que el usuario de demo ya existe en MongoDB
- **CUANDO** el servicio se reinicia y el seed se ejecuta nuevamente
- **ENTONCES** el usuario de demo NO se duplica
- **Y** el servicio arranca sin errores relacionados con el seed

---

### Requisito: Modelo de datos y reglas de negocio

El sistema DEBE persistir usuarios con el modelo definido y aplicar las siguientes reglas
en todos los endpoints:

1. Email único y case-insensitive (normalizado con `trim + lowercase` antes de persistir y comparar).
2. Nombre obligatorio; password mínimo de 8 caracteres.
3. Password hasheado con BCrypt; NUNCA almacenado en texto plano ni expuesto en respuestas.
4. `passwordHash` NUNCA aparece en DTOs ni logs.
5. JWT con expiración; Mongo URI y credenciales configuradas por variables de entorno.
6. Si MongoDB no está disponible, el servicio DEBE fallar de forma explícita; no usar fallback silencioso in-memory.

#### Escenario: passwordHash ausente en todas las respuestas

- **DADO** cualquier operación exitosa (register 201, login 200, me 200)
- **CUANDO** el cliente recibe la respuesta
- **ENTONCES** el cuerpo NO contiene `passwordHash` ni ningún campo que exponga el hash

---

## Fuera de alcance (H2)

- Reset password, MFA, OAuth/SSO
- Verificación de email
- Refresh token rotativo complejo
- Integración con IdP real
- MongoDB para entornos productivos o staging (ver `infra/mongodb-local` para local)

## Notas de consistencia

- El path de registro es `POST /api/v1/auth/register` (definido en H2 página índice).
  La spec `auth/tests-auth-mongodb` (aprobada, H2.3) menciona `POST /api/v1/users`
  como ejemplo en un escenario; el arquitecto DEBE confirmar y alinear el path en su
  diseño técnico antes de implementar los tests.
- El umbral JaCoCo en `auth/tests-auth-mongodb` es LINE ≥ 70% (de la fuente H2.3).
  El stack pack exige 80% de línea como mínimo. El arquitecto decide cuál aplica;
  se recomienda subir a 80% salvo justificación explícita.

## Preguntas abiertas

- **Algoritmo y configuración de JWT**: La fuente indica JWT con expiración pero no define
  el algoritmo de firma (HS256 / RS256), el tiempo de expiración ni los claims exactos.
  Se asume HS256 con expiración configurable por variable de entorno. El arquitecto confirma.
- **Path exacto de `/me`**: La fuente H2 no precisa si el path es `/api/v1/auth/me`
  o `/api/v1/users/me`. Se asume `/api/v1/auth/me` por coherencia con el prefijo del área.
  El arquitecto confirma y alinea con la spec de tests.
- **Activación del seed**: La fuente indica que el seed es "opcional". No se define cómo
  se activa (variable de entorno, perfil Spring, etc.). El arquitecto define el mecanismo.
- **Contrato de error estándar**: La fuente menciona "contrato de error estándar del servicio"
  pero no lo define en H2. Si no está definido en otra spec, el arquitecto lo establece.

# auth-service — uso de la API con curl

Base URL local: `http://localhost:8080`. Todos los endpoints están bajo `/api/v1` (el versionado
de API está deshabilitado en `project.yaml`, por lo que existe una sola versión).

Los ejemplos usan el mismo usuario que Swagger, Postman y Bruno:

| Campo | Valor |
|---|---|
| id | `6710f3a2c9e77c0012345678` |
| name | `Ana Pérez` |
| email | `ana.perez@example.com` |
| password | `password123` (mínimo 8 caracteres) |
| createdAt | `2026-10-22T10:00:00Z` |
| token | `eyJhbGciOiJIUzI1NiJ9...` (marcador de ejemplo; no es un token real) |

> En Windows, `curl` es un alias de `Invoke-WebRequest` en PowerShell 5.1. Usar `curl.exe` para los
> comandos de abajo. En Linux/macOS usar `curl` normalmente.

## Convenciones

- Request y response: `Content-Type: application/json`.
- Fechas: ISO-8601 en UTC (`2026-10-22T10:00:00Z`).
- La contraseña viaja en texto plano solo en el alta y en el login; el servicio la guarda como hash BCrypt.
- El email se normaliza (trim y minúsculas) antes de persistirse.
- Si se repite el mismo email, la API responde `409 CONFLICT`; para volver a probar el alta usar un
  email distinto.
- Los ejemplos de token son marcadores (`eyJhbGciOiJIUzI1NiJ9...`): la API real devuelve un JWT
  firmado distinto en cada alta. No copiar tokens reales a documentación, tickets ni repositorios.

## Configuración JWT

El servicio firma los tokens con HMAC-SHA256 y necesita estas variables de entorno al arrancar:

| Variable | Obligatoria | Para qué | Valor por defecto |
|---|---|---|---|
| `JWT_SECRET` | Sí | Clave HMAC-SHA256; mínimo 32 bytes ASCII | Sin valor: el arranque falla |
| `JWT_EXPIRATION_MS` | No | Vigencia del token en milisegundos | `86400000` (24 horas) |

En local y en Compose se puede partir de `services/auth-service/.env.example`, que contiene una clave
pública exclusiva de desarrollo. Fuera de ese entorno, aprovisionar `JWT_SECRET` con el gestor de
secretos y rotarla si se sospecha exposición. En producción, Helm la toma del Secret externo
`auth-jwt` (ver `delivery/devops-local.md`). No versionar claves reales.

## POST /api/v1/users — registrar usuario

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ana Pérez",
    "email": "ana.perez@example.com",
    "password": "password123"
  }'
```

Respuesta `201 Created` (`AuthResponse`):

```json
{
  "user": {
    "id": "6710f3a2c9e77c0012345678",
    "name": "Ana Pérez",
    "email": "ana.perez@example.com",
    "createdAt": "2026-10-22T10:00:00Z"
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- `user`: perfil público del usuario persistido. No incluye `password` ni `passwordHash`.
- `token`: JWT firmado con HS256. Claims `sub` (id del usuario), `email` (normalizado), `iat` y
  `exp` (`JWT_EXPIRATION_MS`). El valor de este ejemplo es un marcador, no un token real.
- Uso previsto: `Authorization: Bearer <token>` en `GET /api/v1/users/me` y en los servicios que
  consuman el JWT. No hay un endpoint dedicado a validar tokens: la validación ocurre al invocar
  `GET /api/v1/users/me`.
- La cabecera `Location` apunta a `GET /api/v1/users/{id}` con el id creado.

### Errores posibles

`400 VALIDATION_ERROR` — email con formato inválido (o campo obligatorio ausente / contraseña de
menos de 8 caracteres):

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Pérez","email":"invalido","password":"password123"}'
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La solicitud tiene datos inválidos",
  "details": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`400 BAD_REQUEST` — JSON mal formado:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Pérez",'
```

```json
{
  "code": "BAD_REQUEST",
  "message": "Solicitud inválida",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`409 CONFLICT` — el email ya está registrado:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Otra Ana","email":"ana.perez@example.com","password":"password123"}'
```

```json
{
  "code": "CONFLICT",
  "message": "Email ya registrado",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado (no se provoca desde un cliente sano):

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

## POST /api/v1/auth/login — iniciar sesión

Verifica el email normalizado y la contraseña contra el hash BCrypt. Responde el mismo `AuthResponse`
del alta:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ana.perez@example.com",
    "password": "password123"
  }'
```

Respuesta `200 OK` (`AuthResponse`):

```json
{
  "user": {
    "id": "6710f3a2c9e77c0012345678",
    "name": "Ana Pérez",
    "email": "ana.perez@example.com",
    "createdAt": "2026-10-22T10:00:00Z"
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- El email se normaliza (trim y minúsculas) antes de buscar al usuario, igual que en el alta.
- `token` es un JWT HS256 con los mismos claims del alta (`sub`, `email`, `iat`, `exp`). El valor de
  este ejemplo es un marcador: usar el token real que devuelve la respuesta.
- Guardar el token para el pedido siguiente (`GET /api/v1/users/me`).

### Errores posibles

`400 VALIDATION_ERROR` — email mal formado, email o contraseña en blanco o ausentes: mismo JSON
genérico de validación que en el alta (ver arriba).

`401 UNAUTHORIZED` — credenciales incorrectas. El mismo error se devuelve si el email no existe o si
la contraseña no coincide, para no revelar qué parte falló:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.perez@example.com","password":"otraPassword999"}'
```

```json
{
  "code": "UNAUTHORIZED",
  "message": "Credenciales inválidas",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del alta.

## GET /api/v1/users/me — perfil autenticado

Devuelve el perfil persistido del usuario identificado por el `sub` del JWT. Requiere la cabecera
`Authorization` con el token emitido por el alta o el login:

```bash
curl -i http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Respuesta `200 OK` (`UserResponse`):

```json
{
  "id": "6710f3a2c9e77c0012345678",
  "name": "Ana Pérez",
  "email": "ana.perez@example.com",
  "createdAt": "2026-10-22T10:00:00Z"
}
```

- No incluye `password` ni `passwordHash`.
- El servicio verifica firma HS256, expiración y `sub`; no hay endpoint separado de validación.

### Errores posibles

`401 UNAUTHORIZED` — cabecera ausente, sin el prefijo `Bearer`, token mal formado, firma inválida o
token vencido:

```bash
curl -i http://localhost:8080/api/v1/users/me
```

```json
{
  "code": "UNAUTHORIZED",
  "message": "Credenciales inválidas",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`404 NOT_FOUND` — el JWT es válido pero el usuario ya no existe:

```json
{
  "code": "NOT_FOUND",
  "message": "Usuario no encontrado por id: 6710f3a2c9e77c0012345678",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del alta.

## GET /api/v1/users/{id} — consultar usuario

Usar el `id` devuelto por el alta:

```bash
curl -i http://localhost:8080/api/v1/users/6710f3a2c9e77c0012345678
```

Respuesta `200 OK`:

```json
{
  "id": "6710f3a2c9e77c0012345678",
  "name": "Ana Pérez",
  "email": "ana.perez@example.com",
  "createdAt": "2026-10-22T10:00:00Z"
}
```

### Errores posibles

`404 NOT_FOUND` — no existe un usuario con ese id:

```bash
curl -i http://localhost:8080/api/v1/users/6710f3a2c9e77c00ffffffff
```

```json
{
  "code": "NOT_FOUND",
  "message": "Usuario no encontrado por id: 6710f3a2c9e77c00ffffffff",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del alta.

## GET /actuator/health — estado del servicio

```bash
curl -i http://localhost:8080/actuator/health
```

Respuesta `200 OK`:

```json
{
  "status": "UP"
}
```

## Referencias

- Especificación completa: `delivery/openapi/openapi.json` y `delivery/openapi/openapi.yaml`.
- Swagger UI: http://localhost:8080/swagger-ui.html
- Colecciones: `delivery/postman/` y `delivery/bruno/auth-service/`.
- Tests locales con Testcontainers: `delivery/testing.md`.
- Contrato de errores: `src/main/java/com/meridian/users/exception/GlobalExceptionHandler.java`.
- Emisión y validación de JWT: `src/main/java/com/meridian/users/user/service/JwtService.java`.
- Configuración JWT: `src/main/java/com/meridian/users/config/JwtConfig.java` y `application.yml`.

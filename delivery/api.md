# auth-service — uso de la API con curl

> Este documento cubre auth-service y, al final, dashboard-service (puerto 8081). Cada sección es
> independiente y usa la misma convención de ejemplos que Swagger y las colecciones.

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
| accessToken | `eyJhbGciOiJIUzI1NiJ9...` (marcador de ejemplo; no es un token real) |
| tokenType | `Bearer` |
| expiresIn | `3600` (segundos; con `JWT_EXPIRATION_MS` por defecto son 86400) |

> En Windows, `curl` es un alias de `Invoke-WebRequest` en PowerShell 5.1. Usar `curl.exe` para los
> comandos de abajo. En Linux/macOS usar `curl` normalmente.

## Convenciones

- Request y response: `Content-Type: application/json`.
- Fechas: ISO-8601 en UTC (`2026-10-22T10:00:00Z`).
- La contraseña viaja en texto plano solo en el registro y en el login; el servicio la guarda como hash BCrypt.
- El email se normaliza (trim y minúsculas) antes de persistirse.
- Si se repite el mismo email, la API responde `409 CONFLICT`; para volver a probar el registro usar un
  email distinto.
- Los ejemplos de token son marcadores (`eyJhbGciOiJIUzI1NiJ9...`): la API real devuelve un JWT
  firmado distinto en cada registro o login. No copiar tokens reales a documentación, tickets ni
  repositorios.

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

## Usuario de demo (seed)

En los perfiles `dev`, `docker` y `test` (nunca en `prod`) el servicio crea un usuario de demo al
arrancar, de forma idempotente:

| Campo | Valor por defecto |
|---|---|
| email | `demo@example.com` |
| password | Obligatoria mediante `SEED_DEMO_PASSWORD`, sin valor por defecto |
| name | `Demo` |

Se configura con `SEED_DEMO_EMAIL` y `SEED_DEMO_PASSWORD`; se desactiva arrancando sin esos perfiles.
Si la contraseña falta o está vacía, el arranque con seed activo falla. Las credenciales sirven para probar el
login y los endpoints autenticados sin registrar un usuario nuevo.

## POST /api/v1/auth/register — registrar usuario

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
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
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

- `user`: perfil público del usuario persistido. No incluye `password` ni `passwordHash`.
- `accessToken`: JWT firmado con HS256. Claims `sub` (id del usuario), `email` (normalizado), `iat` y
  `exp` (`JWT_EXPIRATION_MS`). El valor de este ejemplo es un marcador, no un token real.
- `tokenType`: siempre `Bearer`. `expiresIn`: vigencia del token en segundos.
- Uso previsto: `Authorization: Bearer <accessToken>` en `GET /api/v1/auth/me` y en los servicios que
  consuman el JWT. No hay un endpoint dedicado a validar tokens: la validación ocurre al invocar
  `GET /api/v1/auth/me`.
- La cabecera `Location` apunta a `GET /api/v1/users/{id}` con el id creado.

### Errores posibles

`400 VALIDATION_ERROR` — email con formato inválido (o campo obligatorio ausente / contraseña de
menos de 8 caracteres):

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
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
curl -i -X POST http://localhost:8080/api/v1/auth/register \
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
curl -i -X POST http://localhost:8080/api/v1/auth/register \
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
del registro:

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
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

- El email se normaliza (trim y minúsculas) antes de buscar al usuario, igual que en el registro.
- `accessToken` es un JWT HS256 con los mismos claims del registro (`sub`, `email`, `iat`, `exp`). El
  valor de este ejemplo es un marcador: usar el token real que devuelve la respuesta.
- `tokenType` es `Bearer` y `expiresIn` la vigencia en segundos.
- Guardar el `accessToken` para el pedido siguiente (`GET /api/v1/auth/me`).

### Errores posibles

`400 VALIDATION_ERROR` — email mal formado, email o contraseña en blanco o ausentes: mismo JSON
genérico de validación que en el registro (ver arriba).

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

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del registro.

## GET /api/v1/auth/me — perfil autenticado

Devuelve el perfil persistido del usuario identificado por el `sub` del JWT. Requiere la cabecera
`Authorization` con el `accessToken` emitido por el registro o el login:

```bash
curl -i http://localhost:8080/api/v1/auth/me \
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
curl -i http://localhost:8080/api/v1/auth/me
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

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del registro.

## POST /api/v1/auth/logout — cerrar sesión

El cliente descarta su `accessToken`. El servidor no revoca JWT en esta implementación: responde
siempre `204 No Content`, con o sin cabecera `Authorization`, incluso con un token inválido.

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Respuesta `204 No Content`, sin cuerpo.

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

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del registro.

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

---

# dashboard-service — uso de la API con curl

Base URL local: `http://localhost:8081` (`PORT` configurable). La API expone una sola versión bajo
`/api/v1`; el versionado está deshabilitado en `project.yaml`.

Los tres endpoints de negocio (`summary`, `charts`, `news`) están **protegidos por JWT**. El antiguo
`GET /api/v1/dashboard` y el DTO `DashboardResponse` fueron retirados.

El servicio se arranca desde `services/dashboard-service`. `JWT_SECRET` es **obligatorio** (mismo valor
que `auth-service`, ≥ 32 bytes, sin default). `FINNHUB_API_KEY` y `NEWS_API_KEY` son opcionales:

```powershell
$env:JWT_SECRET = '<mismo-secreto-que-auth-service>'
$env:DASHBOARD_USE_MOCK = 'true'   # fixtures estáticas, sin proveedores externos
.\mvnw.cmd spring-boot:run
```

### Flujo de autenticación

1. Obtener `accessToken` desde `auth-service` (por ejemplo `POST /api/v1/auth/login`).
2. Enviar `Authorization: Bearer <accessToken>` en cada pedido a `/api/v1/dashboard/*`.

> En Windows, `curl` es un alias de `Invoke-WebRequest` en PowerShell 5.1. Usar `curl.exe` para los
> comandos de abajo. En Linux/macOS usar `curl` normalmente.

## Convenciones

- Request y response: `Content-Type: application/json`.
- Header obligatorio en endpoints de negocio: `Authorization: Bearer <accessToken>`.
- Fechas: ISO-8601 en UTC (`2026-10-22T10:05:00Z`).
- En `summary`, `degraded` es un array (`fx` | `crypto` | `equities`); en `charts`/`news` es boolean.
- Las claves de proveedores se configuran en el entorno del servicio, no viajan en el request.

## GET /api/v1/dashboard/summary — resumen de mercados

```bash
curl.exe -i -H "Authorization: Bearer <accessToken>" http://localhost:8081/api/v1/dashboard/summary
```

Respuesta `200 OK` (`SummaryResponse`, modo mock):

```json
{
  "brand": "Meridian",
  "generatedAt": "2026-10-22T10:05:00Z",
  "fx": {
    "quotes": {
      "usd_oficial": { "compra": 1000, "venta": 1040 },
      "usd_blue": { "compra": 1200, "venta": 1220 }
    }
  },
  "crypto": {
    "bitcoin": { "priceUsd": 67000, "change24hPct": 2.5 },
    "ethereum": { "priceUsd": 3500, "change24hPct": -1.2 }
  },
  "equities": {
    "AAPL": { "price": 175, "currency": "USD" },
    "MSFT": { "price": 415, "currency": "USD" },
    "SPY": { "price": 450, "currency": "USD" }
  },
  "degraded": []
}
```

| Campo | Contenido |
|---|---|
| `fx.quotes` | Dólar ARS oficial/blue (`usd_oficial`, `usd_blue`) — no es Frankfurter |
| `crypto` | Mapas por id (`bitcoin`, `ethereum`) con `priceUsd` y `change24hPct` |
| `equities` | Mapas por ticker (`AAPL`, `MSFT`, `SPY`) con `price` y `currency` |
| `degraded` | Subconjunto de `fx`, `crypto`, `equities` sin datos frescos |

Errores: `401 UNAUTHORIZED` sin JWT válido; `500 INTERNAL_ERROR` ante fallo inesperado de orquestación.

## GET /api/v1/dashboard/charts — serie histórica

```bash
curl.exe -i -H "Authorization: Bearer <accessToken>" "http://localhost:8081/api/v1/dashboard/charts?asset=BTC&range=7d"
```

- `asset` obligatorio: `BTC` | `ETH` | `AAPL` | `MSFT` | `SPY`
- `range` opcional: únicamente `7d` (default)

Respuesta `200 OK` (`ChartsResponse`, extracto modo mock):

```json
{
  "asset": "BTC",
  "range": "7d",
  "points": [
    { "t": "2026-10-15T00:00:00Z", "v": 67000 },
    { "t": "2026-10-22T00:00:00Z", "v": 67000 }
  ],
  "degraded": false
}
```

Errores: `400 VALIDATION_ERROR` si `asset`/`range` no soportado; `401`; `500`.

## GET /api/v1/dashboard/news — noticias IA/tecnología

```bash
curl.exe -i -H "Authorization: Bearer <accessToken>" "http://localhost:8081/api/v1/dashboard/news?topic=ai-tech&limit=8"
```

- `topic` opcional: únicamente `ai-tech` (default)
- `limit` opcional: `1..8` (default `8`)

Respuesta `200 OK` (`NewsResponse`, modo mock):

```json
{
  "items": [
    {
      "id": "demo-ai",
      "title": "AI research demo",
      "summary": "Static AI news fixture",
      "source": "Meridian Demo",
      "publishedAt": "2026-01-01T12:00:00Z",
      "url": "https://example.com/demo-ai",
      "topic": "ai-tech"
    }
  ],
  "degraded": false
}
```

`degraded=true` solo si fallan NewsAPI **y** HN Algolia. Errores: `400`, `401`, `500`.

### Probar con proveedores reales

```powershell
$env:JWT_SECRET = '<mismo-secreto-que-auth-service>'
$env:DASHBOARD_USE_MOCK = 'false'
$env:FINNHUB_API_KEY = '<clave-opcional>'
$env:NEWS_API_KEY = '<clave-opcional>'
.\mvnw.cmd spring-boot:run
```

Los curl son los mismos; cambian valores y, si un proveedor falla, `degraded`.

### Errores comunes

```json
{
  "code": "UNAUTHORIZED",
  "message": "Token JWT inválido o ausente",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

El contrato completo está en `delivery/architecture-overview.md`, sección dashboard-service.

## GET /actuator/health — estado del servicio

```bash
curl.exe -i http://localhost:8081/actuator/health
```

Respuesta `200 OK`:

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

No depende de proveedores externos: un proveedor caído no marca el health como `DOWN`.

## GET /actuator/info — metadatos

```bash
curl.exe -i http://localhost:8081/actuator/info
```

Responde `200 OK`. El contenido depende de las propiedades `info.*` y del build; el servicio no publica
datos sensibles.

## Documentación interactiva

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI completo (JSON) | http://localhost:8081/v3/api-docs |
| Grupo dashboard | http://localhost:8081/v3/api-docs/dashboard |
| Copia versionada | `delivery/openapi/dashboard-service/openapi.json` y `.yaml` |

## Referencias

- Especificación funcional (H3): `specs/dashboard/spec.md` y `specs/dashboard/apis-publicas.md`.
- Arquitectura: `specs/dashboard/arquitectura-apis-publicas.md` y `delivery/architecture-overview.md`.
- Colecciones: `delivery/postman/dashboard-service.postman_collection.json` y
  `delivery/bruno/dashboard-service/` (`2-dashboard/01-summary.bru`, `02-charts.bru`, `03-news.bru`).
- QA paso a paso: `delivery/qa-manual.md`.
- Contrato de error: `services/dashboard-service/src/main/java/com/meridian/dashboard/exception/GlobalExceptionHandler.java`.

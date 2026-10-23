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

El servicio se arranca desde `services/dashboard-service`:

```powershell
$env:DASHBOARD_USE_MOCK = 'true'   # fixtures estáticas, sin proveedores externos
.\mvnw.cmd spring-boot:run
```

Sin `DASHBOARD_USE_MOCK` (o con `false`) consulta los proveedores reales. Los ejemplos de esta sección
son la respuesta real capturada en modo mock; con proveedores activos cambian los valores y `degraded`
puede traer secciones, pero la forma del JSON es la misma.

> En Windows, `curl` es un alias de `Invoke-WebRequest` en PowerShell 5.1. Usar `curl.exe` para los
> comandos de abajo. En Linux/macOS usar `curl` normalmente.

## Convenciones

- Request y response: `Content-Type: application/json`.
- Fechas: ISO-8601 en UTC (`2026-10-15T00:00:00Z`).
- Los precios y tasas son números decimales; `priceUsd` y `change24h` pueden ser enteros o decimales.
- `degraded` siempre es un array; nunca `null`. Las listas de sección pueden venir vacías si no hay datos
  ni caché previa.
- No se envían claves ni tokens al endpoint: las claves de proveedores se configuran en el entorno del
  servicio.

## GET /api/v1/dashboard — dashboard agregado

```bash
curl.exe -i http://localhost:8081/api/v1/dashboard
```

Respuesta `200 OK` (`DashboardResponse`, modo mock):

```json
{
  "dolar": [
    { "tipo": "oficial", "compra": 1000.00, "venta": 1040.00 },
    { "tipo": "blue", "compra": 1200.00, "venta": 1220.00 }
  ],
  "fx": [
    { "from": "USD", "to": "EUR", "rate": 0.92 },
    { "from": "USD", "to": "BRL", "rate": 5.10 }
  ],
  "crypto": [
    { "id": "bitcoin", "symbol": "BTC", "priceUsd": 67000, "change24h": 2.5 },
    { "id": "ethereum", "symbol": "ETH", "priceUsd": 3500, "change24h": -1.2 }
  ],
  "cryptoHistory": [
    { "timestamp": "2026-10-15T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-16T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-17T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-18T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-19T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-20T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-21T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-22T00:00:00Z", "priceUsd": 67000 },
    { "timestamp": "2026-10-15T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-16T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-17T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-18T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-19T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-20T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-21T00:00:00Z", "priceUsd": 3500 },
    { "timestamp": "2026-10-22T00:00:00Z", "priceUsd": 3500 }
  ],
  "equities": [
    { "ticker": "AAPL", "price": 175.00, "currency": "USD" },
    { "ticker": "MSFT", "price": 415.00, "currency": "USD" },
    { "ticker": "SPY", "price": 450.00, "currency": "USD" }
  ],
  "equityCandles": [
    {
      "ticker": "AAPL",
      "ohlcPoints": [
        { "t": 1792627200, "o": 175.00, "h": 175.00, "l": 175.00, "c": 175.00, "v": 0 }
      ]
    },
    {
      "ticker": "MSFT",
      "ohlcPoints": [
        { "t": 1792627200, "o": 415.00, "h": 415.00, "l": 415.00, "c": 415.00, "v": 0 }
      ]
    },
    {
      "ticker": "SPY",
      "ohlcPoints": [
        { "t": 1792627200, "o": 450.00, "h": 450.00, "l": 450.00, "c": 450.00, "v": 0 }
      ]
    }
  ],
  "news": [
    {
      "id": "demo-ai",
      "title": "AI research demo",
      "summary": "Static AI news fixture",
      "source": "Meridian Demo",
      "publishedAt": "2026-01-01T12:00:00Z",
      "url": "https://example.com/demo-ai",
      "topic": "ai-tech"
    },
    {
      "id": "demo-tech",
      "title": "Technology demo",
      "summary": "Static technology news fixture",
      "source": "Meridian Demo",
      "publishedAt": "2026-01-02T12:00:00Z",
      "url": "https://example.com/demo-tech",
      "topic": "ai-tech"
    }
  ],
  "degraded": []
}
```

Campos:

| Sección | Contenido |
|---|---|
| `dolar` | Cotizaciones oficial y blue con `tipo`, `compra` y `venta` en ARS |
| `fx` | Tasas `from` → `to` con `rate` (USD→EUR y USD→BRL) |
| `crypto` | Precio en USD y variación 24 h de BTC y ETH |
| `cryptoHistory` | Serie de 7 días por moneda (`timestamp`, `priceUsd`); BTC primero, ETH después |
| `equities` | Quote de AAPL, MSFT y SPY con `price` y `currency` |
| `equityCandles` | Velas diarias por ticker (`t` Unix, `o`, `h`, `l`, `c`, `v`) |
| `news` | Hasta 8 noticias de IA/tecnología con `id`, `title`, `summary`, `source`, `publishedAt`, `url`, `topic` |
| `degraded` | Nombres de sección sin datos frescos (`dolar`, `fx`, `crypto`, `cryptoHistory`, `equities`, `equityCandles`, `news`) |

Comportamiento con proveedores reales:

- Cada sección se consulta en paralelo y se aísla: un proveedor caído no cambia el `200`.
- Si una sección falla y hay una respuesta anterior en caché, se devuelve esa copia y la sección se
  informa en `degraded`.
- Si falla sin caché, la lista de esa sección viene vacía y también se informa en `degraded`.
- `cryptoHistory` aparece una sola vez en `degraded` aunque fallen BTC y ETH.
- La caché dura `DASHBOARD_CACHE_TTL_SECONDS` (60 s por defecto); CoinGecko usa un mínimo de 60 s.
- Sin `FINNHUB_API_KEY`, `equities` y `equityCandles` usan fixtures estáticas. Sin `NEWS_API_KEY`,
  `news` se consulta a HN Algolia.

### Probar con proveedores reales

Detener el servicio y arrancarlo sin modo mock (las claves son opcionales; no versionar valores reales):

```powershell
$env:DASHBOARD_USE_MOCK = 'false'
$env:FINNHUB_API_KEY = '<clave-opcional>'
$env:NEWS_API_KEY = '<clave-opcional>'
.\mvnw.cmd spring-boot:run
```

El pedido es el mismo:

```bash
curl.exe -i http://localhost:8081/api/v1/dashboard
```

La forma del JSON no cambia; sí los valores y, si algún proveedor falla, el contenido de `degraded`. Sin
conectividad, es esperable un `200` con secciones vacías y `degraded` completo. Para QA reproducible usar
el modo mock.

### Errores posibles

`500 INTERNAL_ERROR` — fallo inesperado de la orquestación (no se provoca desde un cliente sano):

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

El contrato completo (`code`, `message`, `details`, `timestamp`) está en
`delivery/architecture-overview.md`, sección dashboard-service. El endpoint no recibe body ni parámetros,
por lo que no devuelve `400`, `404` ni `409` en el uso normal.

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

- Especificación funcional: `specs/dashboard/apis-publicas.md`.
- Arquitectura: `specs/dashboard/arquitectura-apis-publicas.md` y `delivery/architecture-overview.md`.
- Colecciones: `delivery/postman/dashboard-service.postman_collection.json` y
  `delivery/bruno/dashboard-service/`.
- QA paso a paso: `delivery/qa-manual.md`.
- Contrato de error: `services/dashboard-service/src/main/java/com/meridian/dashboard/exception/GlobalExceptionHandler.java`.

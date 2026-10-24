# auth-service — manual de QA

> Este documento cubre auth-service y, al final, dashboard-service. Cada sección indica cómo arrancar su
> servicio e importar sus colecciones.

Guía para validar el servicio con las colecciones de Postman y Bruno. Todos los pedidos apuntan a
los endpoints reales del código y usan los mismos ejemplos que Swagger y `delivery/api.md`.

## Antes de empezar

1. MongoDB 7 disponible y el servicio arrancado con las variables obligatorias:

   ```powershell
   Set-Location services/auth-service
   $env:MONGO_URI = 'mongodb://localhost:27017'
   $env:MONGO_DATABASE = 'auth_local'
   $env:JWT_SECRET = 'dev-only-jwt-secret-change-me-32-chars-minimum'
   $env:JWT_EXPIRATION_MS = '86400000'
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   .\mvnw.cmd spring-boot:run
   ```

   `JWT_SECRET` es obligatoria (mínimo 32 bytes); en local se puede usar la clave pública de
   `.env.example`. `JWT_EXPIRATION_MS` es opcional (24 horas por defecto). No usar valores reales de
   otros entornos.

2. Verificar que responde:

   ```powershell
   Invoke-WebRequest http://localhost:8080/actuator/health
   ```

   Debe devolver `200` con `{"status":"UP"}`. Si el arranque falla, revisar que `MONGO_URI`,
   `MONGO_DATABASE` y `JWT_SECRET` estén definidas y que MongoDB sea accesible.

3. La colección usa `baseUrl = http://localhost:8080`. Si el servicio corre en otro puerto, cambiar
   la variable `baseUrl` en el entorno.

Docker no es necesario para probar la API con Postman o Bruno: solo hace falta para la suite
automatizada con Testcontainers (ver `delivery/testing.md`).

## Postman

### Importar

1. Abrir Postman → **Import**.
2. Importar `delivery/postman/auth-service.postman_collection.json`.
3. Importar `delivery/postman/auth-service.postman_environment.json`.
4. Seleccionar el entorno **auth-service - local** en el selector superior derecho.

La colección trae variables propias (`baseUrl`, `userId`, `userEmail`, `authToken`). El pedido de
alta genera un email único por corrida, guarda el `id` en `userId` y el JWT en `authToken`; el login
vuelve a guardar su JWT en `authToken` y `GET /me` lo usa como Bearer. Por eso no hace falta editar
nada antes de correr el Collection Runner.

### Orden y resultado esperado

| # | Carpeta / pedido | Método y ruta | Resultado esperado |
|---|---|---|---|
| 1 | 1. Flujo feliz → Registrar usuario | `POST /api/v1/auth/register` | `201`, `AuthResponse`: `user` (`id`, `name`, `email`, `createdAt`), `accessToken` de 3 segmentos, `tokenType = Bearer` y `expiresIn` numérico; guarda `userId`, `userEmail` y `authToken` |
| 2 | 1. Flujo feliz → Iniciar sesión | `POST /api/v1/auth/login` | `200`, `AuthResponse` con el mismo `user.id`, `accessToken` de 3 segmentos, `tokenType = Bearer` y `expiresIn`; guarda `authToken` |
| 3 | 1. Flujo feliz → Consultar usuario por id | `GET /api/v1/users/{{userId}}` | `200`, mismo usuario, sin `password` ni `passwordHash` |
| 4 | 1. Flujo feliz → Consultar mi perfil | `GET /api/v1/auth/me` con `Bearer {{authToken}}` | `200`, perfil del token: `id = {{userId}}`, sin `password` ni `passwordHash` |
| 5 | 1. Flujo feliz → Cerrar sesión | `POST /api/v1/auth/logout` con `Bearer {{authToken}}` | `204`, sin cuerpo (el servidor no revoca el JWT) |
| 6 | 2. Errores → Alta con email inválido | `POST /api/v1/auth/register` | `400`, `code = VALIDATION_ERROR`, `details[0].field = email` |
| 7 | 2. Errores → Alta con JSON mal formado | `POST /api/v1/auth/register` | `400`, `code = BAD_REQUEST`, `details` vacío |
| 8 | 2. Errores → Alta con email duplicado | `POST /api/v1/auth/register` | `409`, `code = CONFLICT`, mensaje `Email ya registrado` |
| 9 | 2. Errores → Consultar usuario inexistente | `GET /api/v1/users/6710f3a2c9e77c00ffffffff` | `404`, `code = NOT_FOUND` |
| 10 | 2. Errores → Login con credenciales inválidas | `POST /api/v1/auth/login` con contraseña incorrecta | `401`, `code = UNAUTHORIZED`, mensaje `Credenciales inválidas`, `details` vacío |
| 11 | 2. Errores → Perfil sin token | `GET /api/v1/auth/me` sin `Authorization` | `401`, `code = UNAUTHORIZED`, `details` vacío |
| 12 | 3. Salud → Health check | `GET /actuator/health` | `200`, `status = UP` |

**Importante:** el pedido 8 reutiliza el email creado en el pedido 1 y el pedido 4 reutiliza su
`accessToken`. Si se ejecutan sueltos, el duplicado puede devolver `400` porque `{{userEmail}}` está
vacío y `/me` puede devolver `401` porque `{{authToken}}` no existe. Correr la colección completa en
orden.

### JWT y perfil autenticado

El registro y el login devuelven `accessToken`, `tokenType` y `expiresIn` además del perfil `user`.
Los tests verifican el status (`201` y `200`), que `accessToken` sea un string no vacío con tres
segmentos separados por puntos, que `tokenType` sea `Bearer`, que `expiresIn` sea numérico, que
`user.id` coincida con el registrado y que no aparezca `password` ni `passwordHash` en la respuesta. El
pedido `GET /api/v1/auth/me` usa `Authorization: Bearer {{authToken}}` y comprueba que devuelve el
perfil del `id` registrado. El logout valida `204` sin cuerpo. Los errores de login y de token ausente
o inválido se validan con `401 UNAUTHORIZED` y `details` vacío. No copiar tokens reales a tickets,
capturas ni repositorios.

En los perfiles `dev`, `docker` y `test` el servicio crea un usuario de demo (`demo@example.com`,
con contraseña obligatoria mediante `SEED_DEMO_PASSWORD`) al arrancar. Se puede repetir el login y `/me` sin registrar un usuario
nuevo; se configura con `SEED_DEMO_EMAIL` y `SEED_DEMO_PASSWORD` y no existe con `prod`.

### Collection Runner

1. En Postman, elegir la colección **auth-service** → **Run**.
2. Seleccionar el entorno **auth-service - local**.
3. Ejecutar en orden (Run auth-service). Todos los tests deben quedar en verde.

La respuesta `500 INTERNAL_ERROR` existe en el contrato y está documentada en Swagger/`api.md`, pero
no se puede provocar desde un cliente sano: no hay un pedido de la colección para forzarla.

### Línea de comandos (opcional)

Si está instalado `newman`:

```powershell
newman run delivery/postman/auth-service.postman_collection.json `
  -e delivery/postman/auth-service.postman_environment.json
```

## Bruno

### Abrir la colección nativa

1. Bruno → **Open Collection**.
2. Elegir la carpeta `delivery/bruno/auth-service/`.
3. Seleccionar el entorno **local** (archivo `environments/local.bru`).
4. Ejecutar la colección completa (**Run Collection**) o los pedidos en el orden del árbol:
   `1-flujo-feliz` → `2-errores` → `3-salud`.

Los pedidos, nombres, orden y datos son los mismos que en Postman. El registro genera `userEmail` y
guarda `userId` y `authToken` (accessToken) en variables de la corrida; el login, `GET /api/v1/auth/me` y `POST /api/v1/auth/logout` reutilizan esas variables, y el pedido de email duplicado depende del registro previo. El token del ejemplo es un marcador: el valor real cambia en cada ejecución. También se puede usar el usuario de demo con la contraseña configurada en `SEED_DEMO_PASSWORD` cuando el seed está activo.

### Importar desde Postman u OpenAPI

Bruno también puede importar `delivery/postman/auth-service.postman_collection.json`
(**Import Collection → Postman Collection**) o `delivery/openapi/openapi.json`
(**Import Collection → OpenAPI**). La colección nativa es la recomendada porque incluye las
aserciones y variables ya configuradas.

## Validación de los archivos

Los JSON deben ser válidos. Con Node.js instalado:

```powershell
node -e "for (const f of process.argv.slice(1)) { JSON.parse(require('fs').readFileSync(f,'utf8')); console.log('OK ' + f); }" delivery/postman/auth-service.postman_collection.json delivery/postman/auth-service.postman_environment.json delivery/openapi/openapi.json delivery/bruno/auth-service/bruno.json
```

## Reiniciar los datos de prueba

Los tests crean usuarios reales en la base configurada. Para dejar la base limpia en local:

```powershell
mongosh $env:MONGO_URI --quiet --eval 'db.getSiblingDB(process.env.MONGO_DATABASE).users.deleteMany({})'
```

No ejecutar en una base compartida o de producción. La colección no necesita datos previos: el alta
genera un email nuevo en cada corrida.

## Problemas frecuentes

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| El servicio no arranca | Falta `MONGO_URI`, `MONGO_DATABASE` o `JWT_SECRET` | Definir las variables obligatorias; no hay valores por defecto |
| El arranque falla por JWT | `JWT_SECRET` mide menos de 32 bytes o no es válida para HS256 | Usar una clave de al menos 32 caracteres; revisar `JwtConfigTest` |
| `Connection refused` en Postman | El servicio no está escuchando en `baseUrl` | Puerto, perfil y logs de arranque |
| `health` devuelve `DOWN` | MongoDB no accesible | `MONGO_URI`, contenedor de Mongo y credenciales del entorno |
| El registro devuelve `409` al repetir | El email ya existe | Usar otro email o limpiar la base de prueba |
| El pedido de duplicado devuelve `400` | Se ejecutó sin el registro previo | Correr la colección completa en orden |
| `GET /api/v1/auth/me` devuelve `401` | Se ejecutó sin login/registro previo o el token venció | Correr la colección completa en orden o repetir el login |
| `verify` falla con `Could not find a valid Docker environment` | Docker no está accesible para Maven | Arrancar Docker y revisar `delivery/testing.md` |
| Swagger UI no abre en `prod` | Deshabilitado a propósito | Usar perfil local/dev o `delivery/openapi/` |

---

# dashboard-service — manual de QA

Guía para validar el servicio con las colecciones de Postman y Bruno. Todos los pedidos apuntan a los
endpoints reales del código (`summary` / `charts` / `news` con JWT) y usan los mismos ejemplos que
Swagger, `delivery/api.md` y el contrato OpenAPI versionado.

## Antes de empezar

1. Tener JDK 21. No hace falta Docker ni base de datos.
2. Tener un `accessToken` válido emitido por `auth-service` (login) y el mismo `JWT_SECRET` en ambos
   servicios (**obligatorio** en dashboard-service, sin default).
3. Arrancar el servicio en modo mock, desde `services/dashboard-service`:

   ```powershell
   $env:PORT = '8081'
   $env:JWT_SECRET = '<mismo-secreto-que-auth-service>'
   $env:DASHBOARD_USE_MOCK = 'true'
   .\mvnw.cmd spring-boot:run
   ```

4. Verificar salud: `Invoke-RestMethod http://localhost:8081/actuator/health` → `status = UP`.
5. Setear `accessToken` en el entorno de Postman/Bruno. `BASE_URL` = `http://localhost:8081`.

`FINNHUB_API_KEY` y `NEWS_API_KEY` son opcionales. No hay datos que limpiar entre corridas.

## Postman

### Importar

1. Abrir Postman → **Import**.
2. Importar `delivery/postman/dashboard-service.postman_collection.json`.
3. Importar `delivery/postman/dashboard-service.postman_environment.json`.
4. Seleccionar el entorno **dashboard-service - local**.
5. Pegar el `accessToken` obtenido desde auth-service en la variable de entorno (vacía por defecto).

| Variable | Valor inicial | Para qué |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | URL base |
| `accessToken` | vacía | JWT de auth-service; se envía como `Authorization: Bearer {{accessToken}}` |
| `FINNHUB_API_KEY` / `NEWS_API_KEY` | vacía | Solo documentan el backend (opcionales) |
| `DASHBOARD_CACHE_TTL_SECONDS` | `60` | Documentación del TTL |
| `DASHBOARD_USE_MOCK` | `true` | Debe coincidir con el arranque del servicio |

### Orden y resultado esperado

| # | Carpeta / pedido | Método y ruta | Resultado esperado |
|---|---|---|---|
| 1 | 1. Salud → Health check | `GET /actuator/health` | `200`, `status = UP` |
| 2 | 2. Dashboard (JWT) → Summary | `GET /api/v1/dashboard/summary` | `200`, `brand`, `fx`, `crypto`, `equities`, `degraded` array |
| 3 | 2. Dashboard (JWT) → Charts | `GET /api/v1/dashboard/charts?asset=BTC&range=7d` | `200`, `asset`, `points` array, `degraded` boolean |
| 4 | 2. Dashboard (JWT) → News | `GET /api/v1/dashboard/news?topic=ai-tech&limit=8` | `200`, `items` array, `degraded` boolean |
| 5 | 3. Documentación → OpenAPI | `GET /v3/api-docs/dashboard` | `200`, paths `summary`/`charts`/`news` (sin `/api/v1/dashboard` único) |
| 6 | 3. Documentación → Swagger UI | `GET /swagger-ui.html` | `200` |

Sin `accessToken` válido, los pedidos 2–4 deben responder `401 UNAUTHORIZED`.

### Collection Runner / newman

```powershell
newman run delivery/postman/dashboard-service.postman_collection.json `
  -e delivery/postman/dashboard-service.postman_environment.json
```

Requiere el servicio arriba, `DASHBOARD_USE_MOCK=true` y `accessToken` cargado en el entorno.

## Bruno

1. Bruno → **Open Collection** → `delivery/bruno/dashboard-service/`.
2. Entorno **local** (`environments/local.bru`): setear `accessToken`.
3. Orden: `1-salud` → `2-dashboard` (`01-summary`, `02-charts`, `03-news`) → `3-documentacion`.

Las aserciones validan status y el shape H3 (no el endpoint único viejo). También se puede importar el
JSON de Postman o el OpenAPI versionado.

## Validación de los archivos

```powershell
node -e "for (const f of process.argv.slice(1)) { JSON.parse(require('fs').readFileSync(f,'utf8')); console.log('OK ' + f); }" delivery/postman/dashboard-service.postman_collection.json delivery/postman/dashboard-service.postman_environment.json delivery/openapi/dashboard-service/openapi.json delivery/bruno/dashboard-service/bruno.json
```

## Problemas frecuentes

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| `401` en summary/charts/news | Token ausente, expirado o `JWT_SECRET` distinto | Login en auth-service; mismo secreto en ambos |
| Servicio no arranca | Falta `JWT_SECRET` | Variable obligatoria sin default |
| Puerto ocupado / connection refused | `8081` en uso o `BASE_URL` incorrecto | Liberar puerto o ajustar `BASE_URL` |
| `degraded` con datos vacíos | Proveedores caídos / sin red | Esperado en modo real; usar mock para QA |
| Swagger UI 404 | springdoc deshabilitado | Entorno local sin `springdoc.swagger-ui.enabled=false` |
| `curl` no acepta `-i` en PowerShell | Alias de `Invoke-WebRequest` | Usar `curl.exe` |

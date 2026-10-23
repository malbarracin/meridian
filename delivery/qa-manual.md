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
endpoints reales del código y usan los mismos ejemplos que Swagger, `delivery/api.md` y el contrato
OpenAPI versionado.

## Antes de empezar

1. Tener JDK 21. No hace falta Docker, base de datos ni claves de proveedores.
2. Arrancar el servicio en modo mock, desde `services/dashboard-service`:

   ```powershell
   $env:PORT = '8081'
   $env:DASHBOARD_USE_MOCK = 'true'
   .\mvnw.cmd spring-boot:run
   ```

   El modo mock devuelve fixtures estáticas y no hace llamadas externas: es el modo recomendado para
   correr la colección completa sin depender de internet ni de cuotas.

3. Verificar que responde:

   ```powershell
   Invoke-RestMethod http://localhost:8081/actuator/health
   ```

   Debe devolver `status = UP`. Si el arranque falla, revisar que el puerto esté libre y que `PORT` sea
   el esperado. `PORT` puede venir definido en el entorno global: por eso el comando lo fija en `8081`.

4. Las colecciones usan `BASE_URL = http://localhost:8081`. Si el servicio corre en otro puerto, cambiar
   `BASE_URL` en el entorno de Postman o en `delivery/bruno/dashboard-service/environments/local.bru`.

No hay datos que limpiar entre corridas: el servicio no persiste nada y la caché es en memoria.

## Postman

### Importar

1. Abrir Postman → **Import**.
2. Importar `delivery/postman/dashboard-service.postman_collection.json`.
3. Importar `delivery/postman/dashboard-service.postman_environment.json`.
4. Seleccionar el entorno **dashboard-service - local** en el selector superior derecho.

El entorno trae:

| Variable | Valor inicial | Para qué |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | URL base de todos los pedidos |
| `FINNHUB_API_KEY` | vacía | Clave de Finnhub para el modo real; no versionar una clave real |
| `NEWS_API_KEY` | vacía | Clave de NewsAPI para el modo real; no versionar una clave real |
| `DASHBOARD_CACHE_TTL_SECONDS` | `60` | TTL usado por el servicio; no se envía en el pedido |
| `DASHBOARD_USE_MOCK` | `true` | Debe coincidir con el modo con el que se arrancó el servicio |

`FINNHUB_API_KEY`, `NEWS_API_KEY` y `DASHBOARD_CACHE_TTL_SECONDS` son configuración del backend: se
cargan al arrancar el servicio, no viajan en las requests. Cambiarlas en Postman no cambia el
comportamiento del servicio; solo documentan el entorno.

### Orden y resultado esperado

| # | Carpeta / pedido | Método y ruta | Resultado esperado |
|---|---|---|---|
| 1 | 1. Salud → Health check | `GET /actuator/health` | `200`, `status = UP` |
| 2 | 2. Dashboard → Dashboard en modo mock | `GET /api/v1/dashboard` | `200`, ocho secciones, `degraded` vacío, 16 puntos de histórico, `dolar[0].tipo = oficial` |
| 3 | 2. Dashboard → Dashboard con proveedores reales | `GET /api/v1/dashboard` | `200`, ocho secciones; valores variables y `degraded` puede no estar vacío |
| 4 | 3. Documentación → OpenAPI del dashboard | `GET /v3/api-docs/dashboard` | `200`, `info.title = Dashboard Service API`, publica `/api/v1/dashboard` |
| 5 | 3. Documentación → Swagger UI | `GET /swagger-ui.html` | `200` (redirección a la interfaz) |

Los pedidos 2 y 3 usan la misma ruta. El modo lo define el arranque del servicio, no la request:

- Con el servicio en mock, correr el pedido 2. El test de mock solo se ejecuta si
  `DASHBOARD_USE_MOCK = true` en el entorno; si no, valida únicamente estructura.
- Para el pedido 3, detener el servicio y volver a arrancarlo con `DASHBOARD_USE_MOCK = 'false'` y,
  opcionalmente, `FINNHUB_API_KEY` y `NEWS_API_KEY`. Cambiar también `DASHBOARD_USE_MOCK` a `false` en
  el entorno de Postman.

Con proveedores reales, valores esperados:

- `dolar`: oficial y blue con valores de mercado.
- `fx`: USD→EUR y USD→BRL.
- `crypto`: BTC y ETH con precio y variación 24 h.
- `cryptoHistory`: serie de 7 días de BTC y de ETH.
- `equities`: AAPL, MSFT y SPY. Sin `FINNHUB_API_KEY`, fixtures (175.00, 415.00 y 450.00 USD).
- `equityCandles`: una o más velas por ticker. Sin key, una vela sintética por ticker.
- `news`: hasta 8 artículos. Sin `NEWS_API_KEY`, noticias de HN Algolia.
- `degraded`: vacío si todos los proveedores responden; si no, los nombres de sección afectados.

El pedido 2 **no** debe correrse contra el servicio en modo real esperando los valores mock: el test
condicional se saltea, pero el ejemplo guardado no coincidirá con la respuesta.

### Collection Runner

1. En Postman, elegir la colección **dashboard-service** → **Run**.
2. Seleccionar el entorno **dashboard-service - local**.
3. Ejecutar en orden (Run dashboard-service). Todos los tests deben quedar en verde.

La respuesta `500 INTERNAL_ERROR` existe en el contrato y está documentada en Swagger y `api.md`, pero
no se puede provocar desde un cliente sano: no hay un pedido de la colección para forzarla.

### Línea de comandos (opcional)

Si está instalado `newman`:

```powershell
newman run delivery/postman/dashboard-service.postman_collection.json `
  -e delivery/postman/dashboard-service.postman_environment.json
```

Para correr solo el modo mock, arrancar el servicio con `DASHBOARD_USE_MOCK=true` y dejar
`DASHBOARD_USE_MOCK=true` en el entorno. El pedido de proveedores reales también pasará si el servicio
está en mock (valida solo estructura), pero no prueba las integraciones.

## Bruno

### Abrir la colección nativa

1. Bruno → **Open Collection**.
2. Elegir la carpeta `delivery/bruno/dashboard-service/`.
3. Seleccionar el entorno **local** (archivo `environments/local.bru`).
4. Ejecutar la colección completa (**Run Collection**) o los pedidos en el orden del árbol:
   `1-salud` → `2-dashboard` → `3-documentacion`.

Los pedidos, nombres, orden y datos son los mismos que en Postman. Las aserciones de Bruno validan
status y que las ocho secciones sean arrays; el caso mock y el real comparten la misma ruta. El entorno
`local.bru` trae `BASE_URL`, `FINNHUB_API_KEY`, `NEWS_API_KEY`, `DASHBOARD_CACHE_TTL_SECONDS` y
`DASHBOARD_USE_MOCK`.

### Importar desde Postman u OpenAPI

Bruno también puede importar `delivery/postman/dashboard-service.postman_collection.json`
(**Import Collection → Postman Collection**) o
`delivery/openapi/dashboard-service/openapi.json` (**Import Collection → OpenAPI**). La colección nativa
es la recomendada porque incluye aserciones y variables ya configuradas.

## Validación de los archivos

Los JSON deben ser válidos. Con Node.js instalado:

```powershell
node -e "for (const f of process.argv.slice(1)) { JSON.parse(require('fs').readFileSync(f,'utf8')); console.log('OK ' + f); }" delivery/postman/dashboard-service.postman_collection.json delivery/postman/dashboard-service.postman_environment.json delivery/openapi/dashboard-service/openapi.json delivery/bruno/dashboard-service/bruno.json
```

## Problemas frecuentes

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| El servicio no arranca por puerto ocupado | Otro proceso usa `8081`, o `PORT` está definido en el entorno | Liberar el puerto o fijar `PORT` a otro valor y ajustar `BASE_URL` |
| `Connection refused` en Postman | El servicio no está escuchando en `BASE_URL` | Puerto, logs de arranque y variable `BASE_URL` |
| `health` responde `DOWN` | El proceso no terminó de arrancar | Esperar el log `Started DashboardApplication` y repetir |
| `degraded` trae todas las secciones | Sin internet, DNS o proveedores bloqueados | Es el comportamiento esperado: `200` con listas vacías; usar modo mock para QA funcional |
| `degraded` trae `equities` o `equityCandles` | Finnhub rechazó la clave o no hay cuota | Revisar `FINNHUB_API_KEY`; sin clave se usan fixtures y no debería degradarse |
| `degraded` trae `news` | NewsAPI y HN fallaron | Revisar `NEWS_API_KEY`, conectividad y cuota; sin clave va directo a HN |
| Los valores mock no coinciden | El servicio no está en modo mock | `DASHBOARD_USE_MOCK=true` al arrancar y en el entorno de Postman |
| El pedido de mock falla en `dolar[0].tipo` | El servicio corre en modo real y el entorno dice mock | Alinear el modo del servicio con `DASHBOARD_USE_MOCK` del entorno |
| Swagger UI devuelve 404 | springdoc deshabilitado en el perfil/entorno | Usar un entorno local sin `springdoc.swagger-ui.enabled=false` |
| `curl` no acepta `-i` en PowerShell | `curl` es alias de `Invoke-WebRequest` | Usar `curl.exe` (ver `delivery/api.md`) |
| El histórico tiene menos de 16 puntos en mock | Se cambió la fixture o el modo | Revisar `DashboardMockFixtures`; en mock son 8 puntos por moneda |

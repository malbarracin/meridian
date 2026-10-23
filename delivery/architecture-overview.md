# auth-service — arquitectura y capas

> Este documento cubre auth-service y, al final, dashboard-service. Cada sección describe el código real
> de su servicio.

Documento de referencia para equipos que integran o mantienen el servicio. Describe la estructura
real del código en `services/auth-service`, no un diseño futuro.

## Identidad del servicio

| Parámetro | Valor | Fuente |
|---|---|---|
| Carpeta | `services/auth-service` | repositorio |
| `groupId` | `com.meridian` | `project.yaml` (`java.group_id`) |
| `artifactId` | `auth-service` | `pom.xml` |
| Paquete base | `com.meridian.users` | `project.yaml` (`java.base_package`) |
| Clase principal | `UsersApplication` | código |
| Java | 21 | `pom.xml` |
| Spring Boot | 3.5.16 | `pom.xml` |
| Persistencia | MongoDB (`spring-boot-starter-data-mongodb`) | `pom.xml` |
| Puerto | `8080` (`PORT` configurable) | `application.yml` |
| Versionado de API | deshabilitado: una sola versión bajo `/api/v1` | `project.yaml` sin `api.versioning` |

## Estructura de paquetes

```text
com.meridian.users
├── UsersApplication.java
├── config/
│   ├── OpenApiConfig.java          # metadatos y ejemplos OpenAPI
│   ├── JwtConfig.java              # jwt.secret y jwt.expiration-ms
│   ├── SecurityConfig.java         # PasswordEncoder BCrypt
│   └── UserIndexConfig.java        # índice único de email en prod
├── exception/                      # contrato de error transversal
│   ├── ErrorResponse.java          # record code/message/details/timestamp
│   ├── FieldError.java             # record field/message
│   ├── ErrorCode.java              # enum de códigos estables
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── NotFoundException.java      # base abstracta
│   ├── ConflictException.java      # base abstracta
│   └── UnauthorizedException.java  # base para credenciales/JWT inválidos
└── user/
    ├── controller/
    │   ├── AuthController.java     # @RequestMapping("/api/v1/auth") → POST /login
    │   └── UserController.java     # @RequestMapping("/api/v1/users") → GET /me y /{id}, POST
    ├── service/
    │   ├── UserService.java        # registro, login y consulta
    │   └── JwtService.java         # emisión y validación HS256
    ├── repository/
    │   └── UserRepository.java     # MongoRepository<User, String>
    ├── mapper/
    │   └── UserMapper.java         # MapStruct: User → UserResponse
    ├── entity/
    │   └── User.java               # @Document(collection = "users")
    ├── dto/
    │   ├── RegisterRequest.java    # name, email, password
    │   ├── LoginRequest.java       # email, password
    │   ├── UserResponse.java       # id, name, email, createdAt
    │   └── AuthResponse.java       # user + token
    └── exception/
        ├── DuplicateUserException.java
        ├── InvalidCredentialsException.java
        └── UserNotFoundException.java
```

La suite de integración H2.3 vive en `src/test/java/com/meridian/users/` (`AuthIntegrationTest` y
`MongoTestContainer`) y en `src/test/java/com/meridian/users/user/integration/`
(`LoginIntegrationTest`, `ProfileIntegrationTest`).

Cada módulo de dominio usa subpaquetes por capa (`controller`, `service`, `repository`, `mapper`,
`entity`, `dto`); el paquete `exception/` de la raíz concentra el contrato de error compartido.

## Responsabilidades por capa

| Capa | Clase | Responsabilidad | No hace |
|---|---|---|---|
| Controller | `UserController`, `AuthController` | Recibe HTTP, valida con `@Valid`, lee `Authorization`, devuelve status y DTO | Reglas de negocio ni mapeo campo a campo |
| Service | `UserService` | Normaliza email, verifica unicidad, hashea contraseña, valida credenciales, resuelve el perfil por JWT y pide el JWT | Conocer HTTP |
| Service | `JwtService` | Firma y valida tokens HS256 con `JwtConfig` | Reglas de negocio de usuarios |
| Repository | `UserRepository` | Persistencia en MongoDB y búsqueda por email | Reglas de negocio |
| Mapper | `UserMapper` | Convierte `User` → `UserResponse` (MapStruct) | Reglas de negocio |
| Entity | `User` | Documento persistido en la colección `users` | Exponerse por la API |
| DTO | `RegisterRequest`, `LoginRequest`, `UserResponse`, `AuthResponse` | Contrato HTTP de entrada y salida | Lógica de negocio |

`User` no se expone: el controller devuelve `AuthResponse` en el alta y en el login (con
`UserResponse` embebido y el JWT) y `UserResponse` en las consultas. Ninguno incluye `passwordHash`.

## Modelo persistido

Colección MongoDB `users`, documento `User`:

| Campo | Tipo | Detalle |
|---|---|---|
| `id` | `String` | `@Id`, ObjectId generado por MongoDB |
| `name` | `String` | Nombre del usuario |
| `email` | `String` | `@Indexed(unique = true)`; se guarda normalizado (trim + minúsculas) |
| `passwordHash` | `String` | Hash BCrypt; nunca el password en claro |
| `createdAt` | `Instant` | Asignado por el servicio al registrar |
| `updatedAt` | `Instant` | Asignado por el servicio al registrar |

La unicidad de email tiene dos defensas: la verificación `findByEmail` del servicio (409 de dominio)
y el índice único de MongoDB (`DuplicateKeyException`, también mapeada a 409). El JWT no se persiste:
se genera en cada alta y en cada login a partir del id y el email del usuario.

## Endpoints

| Método | Ruta | Request | Respuesta OK | Errores |
|---|---|---|---|---|
| POST | `/api/v1/users` | `RegisterRequest` | `201` + `AuthResponse` (`user` + `token`) | 400 `VALIDATION_ERROR`, 400 `BAD_REQUEST`, 409 `CONFLICT`, 500 `INTERNAL_ERROR` |
| POST | `/api/v1/auth/login` | `LoginRequest` | `200` + `AuthResponse` (`user` + `token`) | 400 `VALIDATION_ERROR`, 400 `BAD_REQUEST`, 401 `UNAUTHORIZED`, 500 `INTERNAL_ERROR` |
| GET | `/api/v1/users/me` | `Authorization: Bearer <token>` | `200` + `UserResponse` | 401 `UNAUTHORIZED`, 404 `NOT_FOUND`, 500 `INTERNAL_ERROR` |
| GET | `/api/v1/users/{id}` | `id` en el path | `200` + `UserResponse` | 404 `NOT_FOUND`, 500 `INTERNAL_ERROR` |
| GET | `/actuator/health` | — | `200` + `{"status":"UP"}` | — |

Especificación completa: `delivery/openapi/openapi.json` / `openapi.yaml`.

## Flujo de una petición

### POST /api/v1/users

```text
UserController.registerUser(@Valid RegisterRequest)
  → UserService.register(RegisterRequest)
      normaliza email: trim + minúsculas
      UserRepository.findByEmail(email) → si existe, DuplicateUserException (409)
      PasswordEncoder (BCrypt) → passwordHash
      construye User (name, email normalizado, passwordHash, createdAt, updatedAt)
      UserRepository.save(User) → DuplicateKeyException si el índice unique falla (409)
      UserMapper.toResponse(User)
      JwtService.generateToken(id, email) → JWT HS256 (sub, email, iat, exp)
  ← AuthResponse(UserResponse, token)
← 201 Created + AuthResponse + Location
```

El JWT se firma con `jwt.secret` (`JWT_SECRET`, mínimo 32 bytes) y vence según `jwt.expiration-ms`
(`JWT_EXPIRATION_MS`, 24 horas por defecto). `JwtService.isValid` verifica firma HS256 y expiración;
no hay un endpoint dedicado a validar tokens: la validación ocurre en `GET /api/v1/users/me`.

### POST /api/v1/auth/login

```text
AuthController.login(@Valid LoginRequest)
  → UserService.login(LoginRequest)
      normaliza email: trim + minúsculas
      UserRepository.findByEmail(email) → si no existe, InvalidCredentialsException (401)
      PasswordEncoder.matches(password, passwordHash) → si no coincide, InvalidCredentialsException (401)
      JwtService.generateToken(id, email) → JWT HS256 (sub, email, iat, exp)
      UserMapper.toResponse(User)
  ← AuthResponse(UserResponse, token)
← 200 OK + AuthResponse
```

El mismo `401 UNAUTHORIZED` con mensaje genérico cubre email inexistente y contraseña incorrecta, para
no revelar qué parte de la credencial falló.

### GET /api/v1/users/me

```text
UserController.getMe(Authorization: Bearer <token>)
  si falta la cabecera o no empieza con "Bearer ", InvalidCredentialsException (401)
  → UserService.getByToken(token sin prefijo)
      JwtService.extractUserId(token)
        verifica firma HS256, expiración y subject → si falla, InvalidCredentialsException (401)
      UserService.getById(subject)
        UserRepository.findById(id) → si no existe, UserNotFoundException (404)
        UserMapper.toResponse(User)
  ← 200 OK + UserResponse
```

### GET /api/v1/users/{id}

```text
UserController.getUser(id)
  → UserService.getById(id)
      UserRepository.findById(id)
      si no existe, UserNotFoundException (404)
      UserMapper.toResponse(User)
  ← 200 OK + UserResponse
```

## Contrato de error

Implementado en `exception/GlobalExceptionHandler` (`@RestControllerAdvice`). El mismo JSON para
todas las respuestas de error:

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

| HTTP | `code` | Excepción / caso |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException`, `ConstraintViolationException` |
| 400 | `BAD_REQUEST` | `IllegalArgumentException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` |
| 401 | `UNAUTHORIZED` | `UnauthorizedException` / `InvalidCredentialsException` (credenciales o JWT inválidos) |
| 404 | `NOT_FOUND` | `NotFoundException` / `UserNotFoundException` |
| 409 | `CONFLICT` | `ConflictException` / `DuplicateUserException`, `DuplicateKeyException` de MongoDB |
| 500 | `INTERNAL_ERROR` | Cualquier otra `Exception`; mensaje genérico |

`details` nunca es `null`. El stacktrace y los mensajes internos quedan en el log, no en la respuesta;
`server.error.include-stacktrace` y `include-message` están en `never`.

## Configuración

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGO_URI}
      database: ${MONGO_DATABASE}
      auto-index-creation: false   # true en dev/docker/test

jwt:
  secret: ${JWT_SECRET}                  # obligatoria, mínimo 32 bytes
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}

server:
  port: ${PORT:8080}
  error:
    include-stacktrace: never
    include-message: never
```

`MONGO_URI`, `MONGO_DATABASE` y `JWT_SECRET` son obligatorias y no tienen valor por defecto: si
faltan, el arranque falla explícitamente. No hay base embebida como fallback en ningún perfil.
`JWT_SECRET` no se versiona: en local y Compose se usa la clave pública de `.env.example`; en CI, la
clave de test del workflow; en producción, el Secret externo `auth-jwt` de Helm.

Perfiles:

| Perfil | Índice único de email | Uso |
|---|---|---|
| `dev` | Se crea automáticamente | Desarrollo local |
| `docker` | Se crea automáticamente | Docker Compose |
| `test` | Se crea automáticamente | Tests con MongoDB embebido (Flapdoodle) o Testcontainers |
| `prod` | Desactivado; lo crea el operador | Producción |

En `prod`, Swagger UI y el endpoint de OpenAPI están deshabilitados (`springdoc.swagger-ui.enabled`
y `springdoc.api-docs.enabled` en `false`).

## Tests y calidad

| Capa | Test | Enfoque |
|---|---|---|
| Entidad | `UserTest` | `equals`/`hashCode`/`toString` sin datos sensibles |
| DTO | `RegisterRequestValidationTest` | Restricciones de Bean Validation |
| Repositorio | `UserRepositoryTest` | MongoDB embebido, búsqueda y unicidad |
| Servicio | `UserServiceTest`, `UserRegistrationIntegrationTest` | Normalización, hash, duplicado, login, JWT y flujo real |
| JWT | `JwtConfigTest`, `JwtServiceTest` | Propiedades, firma, claims, expiración y clave corta |
| Mapper | `UserMapperTest` | `User` → `UserResponse` |
| Controller | `UserControllerTest`, `AuthControllerTest`, `ProfileControllerTest` | `@WebMvcTest`: 200/201, 400, 401, 404, 409 y contrato |
| Contrato de error | `ErrorContractHttpTest`, `GlobalExceptionHandlerTest` | Mapeo HTTP y JSON |
| Integración H2.3 | `AuthIntegrationTest`, `LoginIntegrationTest`, `ProfileIntegrationTest` | MongoDB 7.0.14 real con Testcontainers (requiere Docker) |
| Contexto | `UsersApplicationTest` | Arranque con MongoDB embebido |
| OpenAPI | `OpenApiIntegrationTest`, `OpenApiExportTest` | Publicación y export a `delivery/openapi/` |

JaCoCo exige 80 % de cobertura de líneas en `verify`. Comando de cierre:
`./mvnw -q -B verify` desde la raíz o `.\mvnw.cmd clean verify` dentro del servicio. Los tests de
Testcontainers necesitan el daemon Docker accesible; guía en `delivery/testing.md`.

---

# dashboard-service — arquitectura y capas

Documento de referencia para equipos que integran o mantienen el servicio. Describe la estructura real
del código en `services/dashboard-service`, no un diseño futuro. La fuente de requisitos es
`specs/dashboard/arquitectura-apis-publicas.md`.

## Identidad del servicio

| Parámetro | Valor | Fuente |
|---|---|---|
| Carpeta | `services/dashboard-service` | repositorio |
| `groupId` | `com.meridian` | `pom.xml`; coincide con `project.yaml` (`java.group_id`) |
| `artifactId` | `dashboard-service` | `pom.xml` |
| Paquete base | `com.meridian.dashboard` | código (`DashboardApplication`); `project.yaml` declara el base package del auth-service |
| Clase principal | `DashboardApplication` | código |
| Java | 21 | `pom.xml` |
| Spring Boot | 3.5.16 | `pom.xml` |
| Persistencia | ninguna; caché en memoria | `pom.xml` sin motor de datos |
| Puerto | `8081` (`PORT` configurable) | `application.yml` |
| Versionado de API | deshabilitado: una sola versión bajo `/api/v1` | `project.yaml` sin `api.versioning` |
| Cliente HTTP | `WebClient` (spring-boot-starter-webflux) sobre runtime Servlet | `pom.xml` y `WebClientConfig` |

`spring-boot-starter-webflux` se usa solo por `WebClient`: la aplicación sigue siendo Spring MVC
(`spring.main.web-application-type: servlet`) y no expone endpoints reactivos.

## Estructura de paquetes

```text
com.meridian.dashboard
├── DashboardApplication.java
├── config/
│   ├── WebClientConfig.java          # un WebClient por proveedor (base URL + timeouts)
│   ├── DashboardExecutorConfig.java  # ExecutorService con hilos virtuales
│   └── OpenApiConfig.java            # metadatos, ejemplos y grupo OpenAPI
├── exception/                        # contrato de error transversal
│   ├── ErrorResponse.java            # record code/message/details/timestamp
│   ├── FieldError.java               # record field/message
│   ├── ErrorCode.java                # enum de códigos estables
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
│   ├── NotFoundException.java        # base abstracta
│   └── ConflictException.java        # base abstracta
└── dashboard/
    ├── controller/
    │   └── DashboardController.java  # @RequestMapping("/api/v1/dashboard")
    ├── service/
    │   └── DashboardService.java     # orquesta adapters en paralelo y arma degraded
    ├── cache/
    │   └── DashboardCacheService.java # TTL por recurso + última respuesta exitosa
    ├── adapter/
    │   ├── DolarApiAdapter.java      # DolarApi: oficial y blue
    │   ├── FrankfurterAdapter.java   # Frankfurter: USD→EUR/BRL
    │   ├── CoinGeckoAdapter.java     # CoinGecko: precios e histórico 7d
    │   ├── FinnhubAdapter.java       # Finnhub: quotes y velas diarias
    │   ├── NewsAdapter.java          # NewsAPI con fallback HN Algolia
    │   └── *Quote/*Rates/*Feed/*Candles.java # respuestas crudas de proveedores
    ├── mapper/
    │   ├── CurrencyMapper.java       # MapStruct: dólar y FX
    │   ├── CryptoMapper.java         # MapStruct/JsonNode: cripto
    │   ├── EquityMapper.java         # MapStruct: quotes y velas
    │   └── NewsMapper.java           # MapStruct: NewsAPI y HN
    ├── mock/
    │   └── DashboardMockFixtures.java # fixtures estáticas del modo mock
    └── dto/
        ├── DashboardResponse.java
        ├── DolarResponse.java
        ├── FxRateResponse.java
        ├── CryptoResponse.java
        ├── CryptoPricePoint.java
        ├── EquityQuoteResponse.java
        ├── CandleResponse.java
        ├── OhlcPoint.java
        └── NewsArticleResponse.java
```

Cada capa tiene su subpaquete; no hay paquete plano. El módulo `dashboard` no tiene `repository` ni
`entity` porque no persiste datos: la capa de almacenamiento es `cache/` y la de integración es `adapter/`.

## Responsabilidades por capa

| Capa | Clase | Responsabilidad | No hace |
|---|---|---|---|
| Controller | `DashboardController` | Recibe HTTP, devuelve `200` con `DashboardResponse` | Reglas de negocio, llamadas a proveedores ni mapeo |
| Service | `DashboardService` | Lanza las ocho consultas en paralelo, aplica caché, acumula `degraded` y arma la respuesta | Conocer HTTP ni detalles de cada proveedor |
| Cache | `DashboardCacheService` | Guarda por clave la última lista exitosa con TTL; si el proveedor falla y hay entrada previa, la sirve y marca `stale` | Reglas de negocio |
| Adapter | `DolarApiAdapter`, `FrankfurterAdapter`, `CoinGeckoAdapter`, `FinnhubAdapter`, `NewsAdapter` | Consultan su proveedor con `WebClient`, validan la respuesta y la convierten a DTO | Cachear secciones (salvo la caché interna de CoinGecko) ni decidir degraded |
| Mapper | `CurrencyMapper`, `CryptoMapper`, `EquityMapper`, `NewsMapper` | Convierten respuestas crudas a DTO (MapStruct o `JsonNode`) | Llamadas HTTP ni reglas de sección |
| Mock | `DashboardMockFixtures` | Devuelve listas fijas cuando el modo mock está activo o falta la key de Finnhub | Llamadas externas |
| DTO | `DashboardResponse` y DTOs de sección | Contrato HTTP de salida (records inmutables) | Lógica de negocio |

## Modelo de respuesta

`DashboardResponse` es un `record` con ocho listas:

```java
public record DashboardResponse(
        List<DolarResponse> dolar,
        List<FxRateResponse> fx,
        List<CryptoResponse> crypto,
        List<CryptoPricePoint> cryptoHistory,
        List<EquityQuoteResponse> equities,
        List<CandleResponse> equityCandles,
        List<NewsArticleResponse> news,
        List<String> degraded) {
}
```

| Sección | DTO | Campos |
|---|---|---|
| `dolar` | `DolarResponse` | `tipo`, `compra`, `venta` |
| `fx` | `FxRateResponse` | `from`, `to`, `rate` |
| `crypto` | `CryptoResponse` | `id`, `symbol`, `priceUsd`, `change24h` |
| `cryptoHistory` | `CryptoPricePoint` | `timestamp` (ISO-8601 UTC), `priceUsd` |
| `equities` | `EquityQuoteResponse` | `ticker`, `price`, `currency` |
| `equityCandles` | `CandleResponse` / `OhlcPoint` | `ticker`, `ohlcPoints[]` con `t`, `o`, `h`, `l`, `c`, `v` |
| `news` | `NewsArticleResponse` | `id`, `title`, `summary`, `source`, `publishedAt`, `url`, `topic` |
| `degraded` | `List<String>` | nombres de sección sin datos frescos |

Reglas del contrato:

- Ninguna lista es `null`; si no hay datos, es una lista vacía.
- `degraded` nunca es `null`; vacío cuando todas las secciones tienen datos frescos.
- `cryptoHistory` contiene primero la serie de BTC y después la de ETH. Si falla una o ambas, la sección
  se informa una sola vez en `degraded`.
- Las cotizaciones se devuelven normalizadas: dólar oficial y blue, USD→EUR y USD→BRL, BTC y ETH,
  AAPL/MSFT/SPY.

## Flujo de una petición

```text
GET /api/v1/dashboard
  → DashboardController.getDashboard()
      → DashboardService.getDashboard()
          por cada clave: cache.getOrFetch(clave, supplier del adapter) en un hilo virtual
            dolar            → DolarApiAdapter.fetchDolar()
            fx               → FrankfurterAdapter.fetchFx()
            crypto           → CoinGeckoAdapter.fetchPrices()
            cryptoHistory:bitcoin  → CoinGeckoAdapter.fetchHistory("bitcoin")
            cryptoHistory:ethereum → CoinGeckoAdapter.fetchHistory("ethereum")
            equities         → FinnhubAdapter.fetchQuotes()
            equityCandles    → FinnhubAdapter.fetchCandles()
            news             → NewsAdapter.fetchNews()
          CompletableFuture.allOf(...).join()
          cada fallo se convierte en lista vacía + stale=true (no propaga excepción)
          degraded = claves con stale=true, sin duplicados y en orden de sección
      ← DashboardResponse
  ← 200 OK application/json
```

Puntos clave de la orquestación:

- Las consultas son **bloqueantes dentro de cada adaptador** (`WebClient...block()`), pero se ejecutan en
  paralelo en un `ExecutorService` de hilos virtuales (`DashboardExecutorConfig`). El tiempo total se
  aproxima al del proveedor más lento, no a la suma.
- `DashboardService.fetch` captura cualquier fallo con `exceptionally` y lo transforma en
  `CacheResult(List.of(), true)`: un proveedor caído nunca produce un 5xx.
- `degraded` se calcula a partir del flag `stale` de la caché. Si no había entrada previa, la lista de la
  sección queda vacía; si había, se sirve la copia anterior.
- Solo un fallo inesperado fuera de las consultas (por ejemplo, en la construcción de la respuesta) llega
  al `GlobalExceptionHandler` y produce `500 INTERNAL_ERROR`.

### Caché

`DashboardCacheService` usa un `ConcurrentHashMap<String, CacheEntry>`:

- Clave por recurso: `dolar`, `fx`, `crypto`, `cryptoHistory:bitcoin`, `cryptoHistory:ethereum`,
  `equities`, `equityCandles`, `news`.
- TTL: `DASHBOARD_CACHE_TTL_SECONDS` (60 s por defecto). Para claves que empiezan con `crypto` se aplica
  un mínimo de 60 s, para respetar el límite de CoinGecko.
- Dentro del TTL devuelve la lista sin llamar al proveedor.
- Si el TTL venció y el proveedor responde, reemplaza la entrada.
- Si el TTL venció y el proveedor falla, devuelve la entrada anterior con `stale=true`.
- Si falla y no hay entrada previa, propaga el fallo a `DashboardService`, que lo traduce a lista vacía y
  `degraded`.

`CoinGeckoAdapter` además mantiene una caché propia por recurso (`crypto`, `cryptoHistory:bitcoin`,
`cryptoHistory:ethereum`) con el mismo mínimo de 60 s. Es una defensa adicional para no exceder la cuota
del proveedor; la caché de sección sigue siendo la de `DashboardCacheService`.

### Modo mock

Con `DASHBOARD_USE_MOCK=true` cada adaptador devuelve `DashboardMockFixtures` antes de cualquier llamada
HTTP. El servicio sigue pasando por la caché y por el agregador, pero `degraded` queda vacío. Los valores
son ficticios y estáticos; no son cotizaciones reales. `FinnhubAdapter` también usa las fixtures cuando
`FINNHUB_API_KEY` está vacía, aunque el modo mock esté apagado.

## Proveedores

| Sección | Adaptador | Llamada | Autenticación | Fallback |
|---|---|---|---|---|
| `dolar` | `DolarApiAdapter` | `GET /v1/dolares` | ninguna | sin fallback; degrada |
| `fx` | `FrankfurterAdapter` | `GET /latest?from=USD&to=EUR,BRL` | ninguna | sin fallback; degrada |
| `crypto` | `CoinGeckoAdapter` | `GET /api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true` | ninguna | sin fallback; degrada |
| `cryptoHistory` | `CoinGeckoAdapter` | `GET /api/v3/coins/{id}/market_chart?vs_currency=usd&days=7` | ninguna | sin fallback; degrada |
| `equities` | `FinnhubAdapter` | `GET /api/v1/quote?symbol=...&token=...` | `FINNHUB_API_KEY` en query | fixtures si no hay key |
| `equityCandles` | `FinnhubAdapter` | `GET /api/v1/stock/candle?resolution=D&...` | `FINNHUB_API_KEY` en query | fixtures si no hay key |
| `news` | `NewsAdapter` | `GET /v2/everything?...` | `NEWS_API_KEY` en query | HN Algolia `GET /api/v1/search`; si ambos fallan, degrada |

- Cada `WebClient` tiene `base-url` y `timeout-seconds` configurables por proveedor. El timeout se aplica
  a conexión y a respuesta, y además cada llamada tiene un `timeout` de 5 s.
- Los adaptadores validan datos incompletos (por ejemplo, cotización sin compra/venta, tasas faltantes,
  velas con arrays de distinto tamaño) y lanzan excepción para degradar la sección.
- `NewsAdapter` pide hasta 8 artículos, en inglés y ordenados por fecha; sin `NEWS_API_KEY` va directo a
  HN Algolia, y con key intenta NewsAPI y cae a HN ante cualquier error.
- `FinnhubAdapter` consulta AAPL, MSFT y SPY; las velas cubren los últimos siete días con resolución
  diaria.

Limitación conocida: no se verificaron llamadas reales a Finnhub con clave ni la disponibilidad de SPY en
el plan free desde este entorno. SPY se conserva como ETF de referencia y los tests usan MockWebServer.

## Endpoints

| Método | Ruta | Request | Respuesta OK | Errores |
|---|---|---|---|---|
| GET | `/api/v1/dashboard` | ninguno | `200` + `DashboardResponse` | `500 INTERNAL_ERROR` |
| GET | `/actuator/health` | — | `200` + `{"status":"UP","groups":[...]}` | — |
| GET | `/actuator/info` | — | `200` | — |

OpenAPI: `/v3/api-docs` (completo) y `/v3/api-docs/dashboard` (grupo). Swagger UI:
`/swagger-ui.html`. La copia versionada se genera en `delivery/openapi/dashboard-service/openapi.json` y
`.yaml` para no reemplazar el contrato del auth-service, que usa `delivery/openapi/` directamente.

## Contrato de error

Implementado en `exception/GlobalExceptionHandler` (`@RestControllerAdvice`). El mismo JSON para todas las
respuestas de error:

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

| HTTP | `code` | Excepción / caso |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException`, `ConstraintViolationException` |
| 400 | `BAD_REQUEST` | `IllegalArgumentException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` |
| 404 | `NOT_FOUND` | `NotFoundException` |
| 409 | `CONFLICT` | `ConflictException` |
| 500 | `INTERNAL_ERROR` | Cualquier otra `Exception`; mensaje genérico |

El endpoint del dashboard no recibe body ni parámetros, por lo que en el uso normal solo aplica `500`.
Los fallos de proveedores **no** son errores HTTP: se informan en `degraded`. `details` nunca es `null`;
el stacktrace y los mensajes internos quedan en el log. `server.error.include-stacktrace` e
`include-message` están en `never`.

## Configuración

```yaml
spring:
  application:
    name: dashboard-service
  main:
    web-application-type: servlet

server:
  port: ${PORT:8081}
  error:
    include-stacktrace: never
    include-message: never

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true

springdoc:
  api-docs.path: /v3/api-docs
  swagger-ui.path: /swagger-ui.html

dashboard:
  cache:
    ttl-seconds: ${DASHBOARD_CACHE_TTL_SECONDS:60}
  use-mock: ${DASHBOARD_USE_MOCK:false}
  providers:
    dolar-api:
      base-url: ${DASHBOARD_DOLAR_API_BASE_URL:https://dolarapi.com}
      timeout-seconds: ${DASHBOARD_DOLAR_API_TIMEOUT_SECONDS:5}
    frankfurter:
      base-url: ${DASHBOARD_FRANKFURTER_BASE_URL:https://api.frankfurter.app}
      timeout-seconds: ${DASHBOARD_FRANKFURTER_TIMEOUT_SECONDS:5}
    coin-gecko:
      base-url: ${DASHBOARD_COIN_GECKO_BASE_URL:https://api.coingecko.com}
      timeout-seconds: ${DASHBOARD_COIN_GECKO_TIMEOUT_SECONDS:5}
    finnhub:
      base-url: ${DASHBOARD_FINNHUB_BASE_URL:https://finnhub.io}
      timeout-seconds: ${DASHBOARD_FINNHUB_TIMEOUT_SECONDS:5}
      api-key: ${FINNHUB_API_KEY:}
    news-api:
      base-url: ${DASHBOARD_NEWS_API_BASE_URL:https://newsapi.org}
      timeout-seconds: ${DASHBOARD_NEWS_API_TIMEOUT_SECONDS:5}
      api-key: ${NEWS_API_KEY:}
    hn-algolia:
      base-url: ${DASHBOARD_HN_ALGOLIA_BASE_URL:https://hn.algolia.com}
      timeout-seconds: ${DASHBOARD_HN_ALGOLIA_TIMEOUT_SECONDS:5}
```

Ninguna variable es obligatoria: el servicio arranca sin claves y sin base de datos. En producción,
Swagger UI y `/v3/api-docs` se desactivan con `springdoc.swagger-ui.enabled=false` y
`springdoc.api-docs.enabled=false` desde el entorno de despliegue (ver `delivery/devops-local.md`).

## Tests y calidad

| Área | Test | Enfoque |
|---|---|---|
| Agregación | `DashboardServiceTest` | Paralelismo, degraded y respuesta agregada |
| Caché | `DashboardCacheServiceTest` | TTL, reutilización y stale |
| Adaptadores | `DolarApiAdapterTest`, `FrankfurterAdapterTest`, `CoinGeckoAdapterTest`, `FinnhubAdapterTest`, `NewsAdapterTest` | MockWebServer: mapeo, errores, timeouts, fallback y fixtures |
| Controller | `DashboardControllerTest` | `@WebMvcTest`: 200 y contrato |
| Integración | `DashboardMockIntegrationTest` | `@SpringBootTest` en modo mock |
| Contrato de error | `ErrorResponseTest`, `ErrorContractHttpTest` | Mapeo HTTP y JSON |
| Configuración | `DashboardApplicationTest`, `WebClientConfigTest`, `OpenApiConfigTest` | Arranque, timeouts y metadatos OpenAPI |
| OpenAPI | `OpenApiExportTest` | Publicación y export a `delivery/openapi/dashboard-service/` |

JaCoCo exige 80 % de cobertura de líneas en `verify`; solo excluye `DashboardApplication`. Comando de
cierre desde `services/dashboard-service`: `.\mvnw.cmd -q -B verify` (Windows) o `./mvnw -q -B verify`
(Linux/macOS). No requiere Docker ni claves reales.

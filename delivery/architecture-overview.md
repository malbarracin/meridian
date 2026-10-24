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
│   ├── UserIndexConfig.java        # índice único de email en prod
│   └── DemoUserSeeder.java         # seed idempotente (perfiles no productivos)
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
    │   ├── AuthController.java     # /api/v1/auth → register, login, me, logout
    │   └── UserController.java     # /api/v1/users → GET /{id}
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
    │   └── AuthResponse.java       # user, accessToken, tokenType, expiresIn
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
| DTO | `RegisterRequest`, `LoginRequest`, `UserResponse`, `AuthResponse` | Contrato HTTP de entrada y salida (`AuthResponse` con `accessToken`/`tokenType`/`expiresIn`) | Lógica de negocio |

`User` no se expone: el controller devuelve `AuthResponse` en el registro y en el login (con
`UserResponse` embebido, `accessToken`, `tokenType` y `expiresIn`) y `UserResponse` en las consultas.
Ninguno incluye `passwordHash`. El logout no devuelve cuerpo (`204`).

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
| POST | `/api/v1/auth/register` | `RegisterRequest` | `201` + `AuthResponse` (`user`, `accessToken`, `tokenType`, `expiresIn`) | 400 `VALIDATION_ERROR`, 400 `BAD_REQUEST`, 409 `CONFLICT`, 500 `INTERNAL_ERROR` |
| POST | `/api/v1/auth/login` | `LoginRequest` | `200` + `AuthResponse` (`user`, `accessToken`, `tokenType`, `expiresIn`) | 400 `VALIDATION_ERROR`, 400 `BAD_REQUEST`, 401 `UNAUTHORIZED`, 500 `INTERNAL_ERROR` |
| GET | `/api/v1/auth/me` | `Authorization: Bearer <accessToken>` | `200` + `UserResponse` | 401 `UNAUTHORIZED`, 404 `NOT_FOUND`, 500 `INTERNAL_ERROR` |
| POST | `/api/v1/auth/logout` | Opcional `Authorization: Bearer <accessToken>` | `204 No Content` (sin revocación en el servidor) | — |
| GET | `/api/v1/users/{id}` | `id` en el path | `200` + `UserResponse` | 404 `NOT_FOUND`, 500 `INTERNAL_ERROR` |
| GET | `/actuator/health` | — | `200` + `{"status":"UP"}` | — |

Especificación completa: `delivery/openapi/openapi.json` / `openapi.yaml`.

## Flujo de una petición

### POST /api/v1/auth/register

```text
AuthController.registerUser(@Valid RegisterRequest)
  → UserService.register(RegisterRequest)
      normaliza email: trim + minúsculas
      UserRepository.findByEmail(email) → si existe, DuplicateUserException (409)
      PasswordEncoder (BCrypt) → passwordHash
      construye User (name, email normalizado, passwordHash, createdAt, updatedAt)
      UserRepository.save(User) → DuplicateKeyException si el índice unique falla (409)
      UserMapper.toResponse(User)
      JwtService.generateToken(id, email) → JWT HS256 (sub, email, iat, exp)
  ← AuthResponse(UserResponse, accessToken, tokenType, expiresIn)
← 201 Created + AuthResponse + Location
```

El JWT se firma con `jwt.secret` (`JWT_SECRET`, mínimo 32 bytes) y vence según `jwt.expiration-ms`
(`JWT_EXPIRATION_MS`, 24 horas por defecto). `AuthResponse` expone `accessToken`, `tokenType` y
`expiresIn` (calculado en segundos). `JwtService.isValid` verifica firma HS256 y expiración; no hay
un endpoint dedicado a validar tokens: la validación ocurre en `GET /api/v1/auth/me`.

### POST /api/v1/auth/login

```text
AuthController.login(@Valid LoginRequest)
  → UserService.login(LoginRequest)
      normaliza email: trim + minúsculas
      UserRepository.findByEmail(email) → si no existe, InvalidCredentialsException (401)
      PasswordEncoder.matches(password, passwordHash) → si no coincide, InvalidCredentialsException (401)
      JwtService.generateToken(id, email) → JWT HS256 (sub, email, iat, exp)
      UserMapper.toResponse(User)
  ← AuthResponse(UserResponse, accessToken, tokenType, expiresIn)
← 200 OK + AuthResponse
```

El mismo `401 UNAUTHORIZED` con mensaje genérico cubre email inexistente y contraseña incorrecta, para
no revelar qué parte de la credencial falló.

### GET /api/v1/auth/me

```text
AuthController.getMe(Authorization: Bearer <accessToken>)
  si falta la cabecera o no empieza con "Bearer ", InvalidCredentialsException (401)
  → UserService.getByToken(token sin prefijo)
      JwtService.extractUserId(token)
        verifica firma HS256, expiración y subject → si falla, InvalidCredentialsException (401)
      UserService.getById(subject)
        UserRepository.findById(id) → si no existe, UserNotFoundException (404)
        UserMapper.toResponse(User)
  ← 200 OK + UserResponse
```

### POST /api/v1/auth/logout

```text
AuthController.logout()
  → 204 No Content (sin lógica de negocio, sin revocación de JWT en el servidor)
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

`DemoUserSeeder` (`@Profile("(dev | docker | test) & !prod")`) crea al arranque un usuario de demo
idempotente (email por defecto `demo@example.com`, configurable con `SEED_DEMO_EMAIL`).
`SEED_DEMO_PASSWORD` es obligatoria: si falta o está vacía, el arranque falla antes de consultar MongoDB.
Si el email ya existe, no lo modifica. Con `prod` o sin esos perfiles el bean no se registra.

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
del código en `services/dashboard-service`, no un diseño futuro. Fuentes: `specs/dashboard/spec.md` (H3) y
`specs/dashboard/arquitectura-apis-publicas.md`.

## Identidad del servicio

| Parámetro | Valor | Fuente |
|---|---|---|
| Carpeta | `services/dashboard-service` | repositorio |
| `groupId` | `com.meridian` | `pom.xml`; coincide con `project.yaml` (`java.group_id`) |
| `artifactId` | `dashboard-service` | `pom.xml` |
| Paquete base | `com.meridian.dashboard` | código (`DashboardApplication`) |
| Clase principal | `DashboardApplication` | código |
| Java | 21 | `pom.xml` |
| Spring Boot | 3.5.16 | `pom.xml` |
| Persistencia | ninguna; caché en memoria | `pom.xml` sin motor de datos |
| Puerto | `8081` (`PORT` configurable) | `application.yml` |
| Seguridad | JWT HMAC-SHA256 (`JWT_SECRET` obligatorio, alineado con auth-service) | `security/` |
| Versionado de API | deshabilitado: una sola versión bajo `/api/v1` | `project.yaml` sin `api.versioning` |
| Cliente HTTP | `WebClient` (spring-boot-starter-webflux) sobre runtime Servlet | `pom.xml` y `WebClientConfig` |

`spring-boot-starter-webflux` se usa solo por `WebClient`: la aplicación sigue siendo Spring MVC
(`spring.main.web-application-type: servlet`) y no expone endpoints reactivos.

## Estructura de paquetes

```text
com.meridian.dashboard
├── DashboardApplication.java
├── config/
│   ├── WebClientConfig.java          # un WebClient por proveedor
│   ├── DashboardExecutorConfig.java  # ExecutorService con hilos virtuales
│   ├── WebMvcConfig.java             # registra JwtAuthInterceptor
│   └── OpenApiConfig.java            # metadatos, ejemplos y grupo OpenAPI
├── security/
│   ├── JwtConfig.java                # JWT_SECRET obligatorio
│   ├── JwtValidator.java             # valida firma/expiración
│   └── JwtAuthInterceptor.java       # Bearer en /api/v1/dashboard/**
├── exception/                        # ErrorResponse, ErrorCode (+ UNAUTHORIZED), handler
└── dashboard/
    ├── controller/
    │   └── DashboardController.java  # summary | charts | news
    ├── service/
    │   ├── DashboardService.java     # getSummary()
    │   ├── ChartsService.java        # getCharts(asset, range)
    │   ├── NewsService.java          # getNews(topic, limit)
    │   └── SupportedAsset.java       # BTC|ETH|AAPL|MSFT|SPY
    ├── cache/
    │   └── DashboardCacheService.java
    ├── adapter/                      # DolarApi, Frankfurter*, CoinGecko, Finnhub, News
    ├── mapper/
    ├── mock/
    │   └── DashboardMockFixtures.java
    └── dto/
        ├── SummaryResponse.java      # contrato público H3
        ├── ChartsResponse.java
        ├── NewsResponse.java
        ├── FxSection / CryptoSection / EquitiesSection / ...
        └── (DTOs internos de adapters: DolarResponse, CryptoResponse, …)
```

\* Frankfurter se conserva en el código pero **no** participa de `summary` (decisión H3).

## Responsabilidades por capa

| Capa | Clase | Responsabilidad | No hace |
|---|---|---|---|
| Controller | `DashboardController` | HTTP + OpenAPI; delega a services | Orquestar proveedores |
| Service | `DashboardService`, `ChartsService`, `NewsService` | Agregación, validación de params, `degraded` | Conocer HTTP |
| Security | `JwtAuthInterceptor` | Exige Bearer JWT válido en `/api/v1/dashboard/**` | Emitir tokens (eso es auth-service) |
| Cache | `DashboardCacheService` | TTL + última respuesta buena | Reglas de negocio |
| Adapter | `*Adapter` | WebClient + validación + mapeo a DTO interno | Decidir el contrato público |
| DTO público | `SummaryResponse`, `ChartsResponse`, `NewsResponse` | Contrato HTTP H3 | Lógica de negocio |

## Modelo de respuesta (contrato H3)

```java
SummaryResponse(brand, generatedAt, FxSection fx, CryptoSection crypto, EquitiesSection equities, List<String> degraded)
ChartsResponse(asset, range, List<ChartPoint> points, boolean degraded)
NewsResponse(List<NewsArticleResponse> items, boolean degraded)
```

- Público `fx` = dólar ARS (`usd_oficial` / `usd_blue`), no Frankfurter.
- `CryptoSection` / `EquitiesSection` se serializan como mapas planos (`@JsonValue`), sin wrapper `values`.
- `degraded` en summary: subconjunto de `fx`, `crypto`, `equities`. En charts/news: boolean.

El DTO plano `DashboardResponse` y `GET /api/v1/dashboard` **fueron retirados**.

## Flujo de una petición

```text
GET /api/v1/dashboard/summary|charts|news
  → JwtAuthInterceptor (401 si falta/inválido)
  → DashboardController
      → DashboardService.getSummary()     // dolar + crypto + equities en paralelo
      → ChartsService.getCharts(asset, range)
      → NewsService.getNews(topic, limit)
  ← 200 + SummaryResponse | ChartsResponse | NewsResponse
```

Un proveedor caído no produce 5xx: se marca `degraded` y se sirve stale o vacío. Solo un fallo
inesperado de orquestación llega al `GlobalExceptionHandler` como `500 INTERNAL_ERROR`.

### Modo mock

Con `DASHBOARD_USE_MOCK=true` los adapters usan `DashboardMockFixtures`. `FINNHUB_API_KEY` /
`NEWS_API_KEY` siguen siendo opcionales (fixtures / HN Algolia).

## Proveedores usados por endpoint

| Endpoint | Fuentes |
|---|---|
| `summary` | DolarApi (`fx`), CoinGecko (`crypto`), Finnhub (`equities`) |
| `charts` | CoinGecko (BTC/ETH) o Finnhub candles (AAPL/MSFT/SPY) |
| `news` | NewsAPI → fallback HN Algolia |

## Endpoints

| Método | Ruta | Auth | Respuesta OK | Errores |
|---|---|---|---|---|
| GET | `/api/v1/dashboard/summary` | Bearer JWT | `200` `SummaryResponse` | `401`, `500` |
| GET | `/api/v1/dashboard/charts` | Bearer JWT | `200` `ChartsResponse` | `400`, `401`, `500` |
| GET | `/api/v1/dashboard/news` | Bearer JWT | `200` `NewsResponse` | `400`, `401`, `500` |
| GET | `/actuator/health` | — | `200` | — |
| GET | `/actuator/info` | — | `200` | — |

OpenAPI: `/v3/api-docs` y `/v3/api-docs/dashboard`. Copia versionada:
`delivery/openapi/dashboard-service/openapi.json` y `.yaml`.

## Contrato de error

Mismo JSON (`code`, `message`, `details`, `timestamp`) para toda la API. Códigos: `VALIDATION_ERROR`,
`BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED` (401), `INTERNAL_ERROR`. Los fallos de proveedores
**no** son errores HTTP: van en `degraded`.

## Configuración relevante

| Variable | Obligatorio | Notas |
|---|---|---|
| `JWT_SECRET` | **sí** | Sin default; ≥ 32 bytes; igual que auth-service |
| `DASHBOARD_USE_MOCK` | no | `false` por defecto |
| `FINNHUB_API_KEY` / `NEWS_API_KEY` | no | Opcionales (fixtures / HN) |
| `DASHBOARD_CACHE_TTL_SECONDS` | no | default `60` |

En producción se desactivan Swagger UI y `/v3/api-docs` (ver `delivery/devops-local.md`).

## Tests y calidad

| Área | Test |
|---|---|
| Summary / charts / news | `DashboardServiceTest`, `ChartsService*`, `NewsService*`, `DashboardControllerTest` |
| JWT | `JwtHttpIntegrationTest` |
| Adaptadores | `*AdapterTest` (MockWebServer) |
| OpenAPI | `OpenApiExportTest` (assert: sin `/api/v1/dashboard` ni `DashboardResponse`) |

JaCoCo 80 %. Cierre: desde `services/dashboard-service`, `.\mvnw.cmd -q -B verify`.

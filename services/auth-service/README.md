# auth-service — servicio de autenticación

Servicio REST que registra usuarios, autentica credenciales y consulta los datos públicos de usuarios,
con persistencia en MongoDB. Normaliza el email antes de guardarlo, garantiza su unicidad, hashea la
contraseña con BCrypt y emite un JWT de acceso firmado (HS256) en el alta y en el login. El perfil
autenticado se consulta con ese token. Nunca devuelve la contraseña ni su hash. Forma parte del sistema
Meridian y es consumido por otros equipos para el alta, el login y la consulta de usuarios.

## Funcionalidades

- Alta de usuario: `POST /api/v1/users` (nombre, email y contraseña) → `AuthResponse` con el perfil
  público (`user`) y el JWT de acceso (`token`).
- Login: `POST /api/v1/auth/login` (email y contraseña) → `200` con el mismo `AuthResponse`; las
  credenciales incorrectas responden `401 UNAUTHORIZED` sin revelar si el email existe.
- Perfil autenticado: `GET /api/v1/users/me` con `Authorization: Bearer <token>` → perfil público del
  usuario del JWT.
- Consulta de usuario por id: `GET /api/v1/users/{id}`.
- Health check: `GET /actuator/health`.

## Stack

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Base de datos | MongoDB 7 (Flapdoodle en tests unitarios y Testcontainers en los de integración) |
| Build | Maven (wrapper incluido) |
| Documentación API | springdoc-openapi 2.8.17 (Swagger UI) |

## Requisitos

- JDK 21.
- MongoDB 7 accesible (local, contenedor o instancia administrada).
- Docker accesible para la suite de integración con Testcontainers (`verify`); ver
  [`delivery/testing.md`](../../delivery/testing.md).
- Docker con Compose v2 si se usa el `docker-compose.yml` del servicio.
- No hace falta instalar Maven: se usa `./mvnw` (`.\mvnw.cmd` en Windows).

## Cómo correrlo

### Local

Definir las variables obligatorias y arrancar con el perfil `dev` (habilita la creación automática
del índice único de email):

```powershell
Set-Location services/auth-service
$env:MONGO_URI = 'mongodb://localhost:27017'
$env:MONGO_DATABASE = 'auth_local'
$env:JWT_SECRET = '<clave-local-de-al-menos-32-caracteres>'
$env:JWT_EXPIRATION_MS = '86400000'
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
cd services/auth-service
export MONGO_URI=mongodb://localhost:27017
export MONGO_DATABASE=auth_local
export JWT_SECRET='<clave-local-de-al-menos-32-caracteres>'
export JWT_EXPIRATION_MS=86400000
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Sin `MONGO_URI`, `MONGO_DATABASE` y `JWT_SECRET` el arranque falla con un error explícito: no hay
conexión ni clave por defecto, y no hay base embebida de respaldo. `.env.example` trae una clave
pública de desarrollo; nunca usar ese valor fuera del entorno local.

### Con Docker

El servicio incluye `docker-compose.yml` con MongoDB 7 y la aplicación:

```powershell
Set-Location services/auth-service
Copy-Item .env.example .env
docker compose up --build -d --wait
Invoke-WebRequest http://localhost:8080/actuator/health
```

`.env.example` incluye `JWT_SECRET` y `JWT_EXPIRATION_MS`; Compose usa la clave pública de
desarrollo si no se define otra. El detalle de Compose, CI, Helm y despliegue está en
`delivery/devops-local.md`. No usar el Compose como configuración de producción.

## Configuración

| Variable / propiedad | Para qué | Valor por defecto |
|---|---|---|
| `MONGO_URI` | URI de conexión a MongoDB | Sin valor: obligatoria |
| `MONGO_DATABASE` | Nombre de la base de datos | Sin valor: obligatoria |
| `JWT_SECRET` | Clave HMAC-SHA256 para firmar los JWT; mínimo 32 bytes | Sin valor: obligatoria |
| `JWT_EXPIRATION_MS` | Vigencia del JWT en milisegundos | `86400000` (24 horas) |
| `PORT` | Puerto HTTP del servicio | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev`, `docker`, `test`, `prod`) | Sin perfil |
| `springdoc.swagger-ui.enabled` | Swagger UI | `false` en el perfil `prod` |
| `springdoc.api-docs.enabled` | Endpoint `/v3/api-docs` | `false` en el perfil `prod` |

`JWT_SECRET` debe ser privada y distinta por entorno: no versionar valores reales. En desarrollo se
puede usar la clave pública de `.env.example`; los tests usan la fixture pública de
`src/test/resources/application.yml`.

Perfiles: `dev` y `docker` crean el índice único de email automáticamente; `prod` lo mantiene
desactivado y el índice se crea con el script de operación descrito en `delivery/devops-local.md`.

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs · copia estática en
  [`delivery/openapi/openapi.json`](../../delivery/openapi/openapi.json) y
  [`openapi.yaml`](../../delivery/openapi/openapi.yaml)

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| POST | `/api/v1/users` | Registra un usuario y devuelve `AuthResponse`: perfil público + JWT | 201, 400, 409, 500 |
| POST | `/api/v1/auth/login` | Verifica email y contraseña y devuelve `AuthResponse` | 200, 400, 401, 500 |
| GET | `/api/v1/users/me` | Devuelve el perfil del JWT (`Authorization: Bearer`) | 200, 401, 404, 500 |
| GET | `/api/v1/users/{id}` | Consulta un usuario por id | 200, 404, 500 |
| GET | `/actuator/health` | Estado del servicio | 200 |

Los endpoints viven bajo `/api/v1`; el versionado de API está deshabilitado en `project.yaml`.

### Token JWT

El alta responde `201 Created` con un `AuthResponse`:

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

- Algoritmo de firma: HMAC-SHA256 (HS256), con la clave de `JWT_SECRET`.
- Claims: `sub` = id del usuario, `email` = email normalizado, `iat` y `exp` (vigencia de
  `JWT_EXPIRATION_MS`).
- Uso previsto: cabecera `Authorization: Bearer <token>` en `GET /api/v1/users/me` y en los servicios
  que consuman el JWT. No hay un endpoint dedicado a validar tokens: la validación ocurre al invocar
  `GET /api/v1/users/me`.
- `user` nunca incluye la contraseña ni `passwordHash`; el `token` del ejemplo es un marcador, no un
  token real.

### Login y perfil autenticado

`POST /api/v1/auth/login` verifica el email (normalizado igual que en el alta) y la contraseña contra
el hash BCrypt. Responde `200 OK` con el mismo `AuthResponse` del alta:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ana.perez@example.com",
    "password": "password123"
  }'
```

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

Con ese token, `GET /api/v1/users/me` devuelve el perfil persistido del usuario del claim `sub`:

```bash
curl -i http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

```json
{
  "id": "6710f3a2c9e77c0012345678",
  "name": "Ana Pérez",
  "email": "ana.perez@example.com",
  "createdAt": "2026-10-22T10:00:00Z"
}
```

- Credenciales incorrectas (email inexistente o contraseña que no coincide) → `401 UNAUTHORIZED` con
  el mismo mensaje genérico, sin revelar si el email existe.
- `Authorization` ausente, sin el prefijo `Bearer`, con firma inválida o con el token vencido →
  `401 UNAUTHORIZED`.
- Token válido pero usuario ya eliminado → `404 NOT_FOUND`.
- Reemplazar el token de los ejemplos por el `token` real que devuelve el login o el alta.

### Errores

Todos los errores usan el mismo JSON (`code`, `message`, `details`, `timestamp`). `details` siempre
es una lista (vacía si no aplica) y nunca se expone stacktrace ni detalle interno.

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

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campos inválidos o ausentes en el cuerpo |
| 400 | `BAD_REQUEST` | JSON mal formado, parámetro de tipo inválido o faltante |
| 401 | `UNAUTHORIZED` | Credenciales incorrectas o JWT ausente, inválido o vencido |
| 404 | `NOT_FOUND` | Usuario inexistente |
| 409 | `CONFLICT` | Email ya registrado (también si el índice único de MongoDB rechaza el documento) |
| 500 | `INTERNAL_ERROR` | Error inesperado; mensaje genérico |

## Probar la API

- Postman: importar [`delivery/postman/auth-service.postman_collection.json`](../../delivery/postman/auth-service.postman_collection.json)
  y el entorno [`delivery/postman/auth-service.postman_environment.json`](../../delivery/postman/auth-service.postman_environment.json).
- Bruno: abrir la colección [`delivery/bruno/auth-service/`](../../delivery/bruno/auth-service/).
- Uso con curl: [`delivery/api.md`](../../delivery/api.md).
- Paso a paso para QA: [`delivery/qa-manual.md`](../../delivery/qa-manual.md).

Los ejemplos de Swagger, `delivery/api.md`, Postman y Bruno usan el mismo usuario de prueba:
`Ana Pérez`, `ana.perez@example.com`, contraseña `password123`, id
`6710f3a2c9e77c0012345678`. Las colecciones crean un email único por corrida para el alta y reutilizan
el JWT emitido en el login y en `GET /api/v1/users/me`.

## Tests

```powershell
Set-Location services/auth-service
.\mvnw.cmd clean verify
```

Linux/macOS: `./mvnw clean verify`. Desde la raíz del repositorio, `./mvnw -q -B verify` delega al
wrapper del servicio.

La suite combina dos enfoques:

- Unitarios y de contexto (entidad, repositorio, servicio, mapper, controllers con `MockMvc`,
  contrato de error, configuración y JWT): MongoDB embebido Flapdoodle o mocks, sin Docker.
- Integración H2.3 (`AuthIntegrationTest`, `LoginIntegrationTest`, `ProfileIntegrationTest`):
  MongoDB 7.0.14 real levantado por Testcontainers (`MongoTestContainer`), con el registro, el login
  y `GET /me` ejercitados por HTTP.

**Docker es obligatorio para `verify`:** el daemon debe estar accesible para el usuario que corre
Maven (`docker info` responde). Si no lo está, los tests de Testcontainers fallan al inicializar el
contenedor; no se omiten. La primera corrida descarga `mongo:7.0.14` y las imágenes auxiliares de
Testcontainers. Guía, comandos por clase y diagnóstico en
[`delivery/testing.md`](../../delivery/testing.md).

No exportar `JWT_SECRET` de forma global al correr los tests: `JwtConfigTest` comprueba el arranque
sin clave y la fixture `src/test/resources/application.yml` aporta los valores públicos del resto de
la suite. JaCoCo exige un mínimo de 80 % de cobertura de líneas y el reporte queda en
`target/site/jacoco/index.html`. El build también regenera `delivery/openapi/openapi.json` y
`openapi.yaml` mediante `OpenApiExportTest`.

## Estructura del proyecto

```text
services/auth-service/
├── pom.xml
├── mvnw / mvnw.cmd
├── docker-compose.yml
└── src/main/java/com/meridian/users/
    ├── UsersApplication.java
    ├── config/       # OpenApiConfig, JwtConfig, SecurityConfig y UserIndexConfig
    ├── exception/    # contrato de error global
    └── user/
        ├── controller/   # AuthController y UserController
        ├── service/      # UserService y JwtService (emisión/validación HS256)
        ├── repository/   # UserRepository (MongoRepository)
        ├── mapper/       # UserMapper (MapStruct)
        ├── entity/       # User (@Document "users")
        ├── dto/          # RegisterRequest, LoginRequest, UserResponse, AuthResponse
        └── exception/    # DuplicateUserException, UserNotFoundException, InvalidCredentialsException
```

Los tests de integración H2.3 viven en `src/test/java/com/meridian/users/` y en
`src/test/java/com/meridian/users/user/integration/`; `MongoTestContainer` centraliza el contenedor
MongoDB compartido por la JVM.

## Documentación

| Documento | Para qué |
|---|---|
| [`delivery/api.md`](../../delivery/api.md) | Uso de la API con ejemplos curl |
| [`delivery/architecture-overview.md`](../../delivery/architecture-overview.md) | Capas, paquetes, modelo y flujo de una petición |
| [`delivery/qa-manual.md`](../../delivery/qa-manual.md) | Cómo importar y correr Postman/Bruno |
| [`delivery/openapi/openapi.json`](../../delivery/openapi/openapi.json) | Especificación OpenAPI generada por el build |
| [`delivery/testing.md`](../../delivery/testing.md) | Tests locales con Testcontainers y diagnóstico de Docker |
| [`delivery/devops-local.md`](../../delivery/devops-local.md) | Docker, CI y despliegue |
| [`specs/auth/persistencia.md`](../../specs/auth/persistencia.md) | Requisitos de negocio (fuente) |
| [`specs/auth/arquitectura.md`](../../specs/auth/arquitectura.md) | Diseño técnico de referencia |

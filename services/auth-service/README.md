# auth-service — servicio de autenticación

Servicio REST que registra y consulta los datos públicos de usuarios, con persistencia en MongoDB.
Normaliza el email antes de guardarlo, garantiza su unicidad y nunca devuelve la contraseña ni su hash.
Forma parte del sistema Meridian y es consumido por otros equipos para el alta y la consulta de usuarios.

## Funcionalidades

- Alta de usuario: `POST /api/v1/users` (nombre, email y contraseña).
- Consulta de usuario por id: `GET /api/v1/users/{id}`.
- Health check: `GET /actuator/health`.

## Stack

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Base de datos | MongoDB 7 (MongoDB embebido Flapdoodle en tests) |
| Build | Maven (wrapper incluido) |
| Documentación API | springdoc-openapi 2.8.17 (Swagger UI) |

## Requisitos

- JDK 21.
- MongoDB 7 accesible (local, contenedor o instancia administrada).
- Docker con Compose v2 solo si se usa el `docker-compose.yml` del servicio.
- No hace falta instalar Maven: se usa `./mvnw` (`.\mvnw.cmd` en Windows).

## Cómo correrlo

### Local

Definir las variables obligatorias y arrancar con el perfil `dev` (habilita la creación automática
del índice único de email):

```powershell
Set-Location services/auth-service
$env:MONGO_URI = 'mongodb://localhost:27017'
$env:MONGO_DATABASE = 'auth_local'
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
cd services/auth-service
export MONGO_URI=mongodb://localhost:27017
export MONGO_DATABASE=auth_local
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Sin `MONGO_URI` y `MONGO_DATABASE` el arranque falla con un error explícito: no hay conexión por
defecto ni base embebida de respaldo.

### Con Docker

El servicio incluye `docker-compose.yml` con MongoDB 7 y la aplicación:

```powershell
Set-Location services/auth-service
Copy-Item .env.example .env
docker compose up --build -d --wait
Invoke-WebRequest http://localhost:8080/actuator/health
```

El detalle de Compose, CI, Helm y despliegue está en `delivery/devops-local.md`.
No usar el Compose como configuración de producción.

## Configuración

| Variable / propiedad | Para qué | Valor por defecto |
|---|---|---|
| `MONGO_URI` | URI de conexión a MongoDB | Sin valor: obligatoria |
| `MONGO_DATABASE` | Nombre de la base de datos | Sin valor: obligatoria |
| `PORT` | Puerto HTTP del servicio | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev`, `docker`, `test`, `prod`) | Sin perfil |
| `springdoc.swagger-ui.enabled` | Swagger UI | `false` en el perfil `prod` |
| `springdoc.api-docs.enabled` | Endpoint `/v3/api-docs` | `false` en el perfil `prod` |

Perfiles: `dev` y `docker` crean el índice único de email automáticamente; `prod` lo mantiene
desactivado y el índice se crea con el script de operación descrito en `delivery/devops-local.md`.

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs · copia estática en
  [`delivery/openapi/openapi.json`](../../delivery/openapi/openapi.json) y
  [`openapi.yaml`](../../delivery/openapi/openapi.yaml)

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| POST | `/api/v1/users` | Registra un usuario y devuelve sus datos públicos | 201, 400, 409, 500 |
| GET | `/api/v1/users/{id}` | Consulta un usuario por id | 200, 404, 500 |
| GET | `/actuator/health` | Estado del servicio | 200 |

Los endpoints viven bajo `/api/v1`; el versionado de API está deshabilitado en `project.yaml`.

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
`Ana Pérez`, `ana.perez@example.com`, id `6710f3a2c9e77c0012345678`.

## Tests

```powershell
Set-Location services/auth-service
.\mvnw.cmd clean verify
```

Linux/macOS: `./mvnw clean verify`. Desde la raíz del repositorio, `./mvnw -q -B verify` delega al
wrapper del servicio.

La suite cubre entidad y repositorio contra MongoDB embebido, servicio, mapper, controller con
`MockMvc`, contrato de error y contexto completo. JaCoCo exige un mínimo de 80 % de cobertura de
líneas y el reporte queda en `target/site/jacoco/index.html`. El build también regenera
`delivery/openapi/openapi.json` y `openapi.yaml` mediante `OpenApiExportTest`.

## Estructura del proyecto

```text
services/auth-service/
├── pom.xml
├── mvnw / mvnw.cmd
├── docker-compose.yml
└── src/main/java/com/meridian/users/
    ├── UsersApplication.java
    ├── config/       # OpenApiConfig y PasswordEncoder BCrypt
    ├── exception/    # contrato de error global
    └── user/
        ├── controller/   # UserController
        ├── service/      # UserService
        ├── repository/   # UserRepository (MongoRepository)
        ├── mapper/       # UserMapper (MapStruct)
        ├── entity/       # User (@Document "users")
        ├── dto/          # RegisterRequest, UserResponse
        └── exception/    # DuplicateUserException, UserNotFoundException
```

## Documentación

| Documento | Para qué |
|---|---|
| [`delivery/api.md`](../../delivery/api.md) | Uso de la API con ejemplos curl |
| [`delivery/architecture-overview.md`](../../delivery/architecture-overview.md) | Capas, paquetes, modelo y flujo de una petición |
| [`delivery/qa-manual.md`](../../delivery/qa-manual.md) | Cómo importar y correr Postman/Bruno |
| [`delivery/openapi/openapi.json`](../../delivery/openapi/openapi.json) | Especificación OpenAPI generada por el build |
| [`delivery/devops-local.md`](../../delivery/devops-local.md) | Docker, CI y despliegue |
| [`specs/auth/persistencia.md`](../../specs/auth/persistencia.md) | Requisitos de negocio (fuente) |
| [`specs/auth/arquitectura.md`](../../specs/auth/arquitectura.md) | Diseño técnico de referencia |

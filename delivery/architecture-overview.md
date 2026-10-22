# auth-service — arquitectura y capas

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
│   └── SecurityConfig.java         # PasswordEncoder BCrypt
├── exception/                      # contrato de error transversal
│   ├── ErrorResponse.java          # record code/message/details/timestamp
│   ├── FieldError.java             # record field/message
│   ├── ErrorCode.java              # enum de códigos estables
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── NotFoundException.java      # base abstracta
│   └── ConflictException.java      # base abstracta
└── user/
    ├── controller/
    │   └── UserController.java     # @RequestMapping("/api/v1/users")
    ├── service/
    │   └── UserService.java        # reglas de registro y consulta
    ├── repository/
    │   └── UserRepository.java     # MongoRepository<User, String>
    ├── mapper/
    │   └── UserMapper.java         # MapStruct: User → UserResponse
    ├── entity/
    │   └── User.java               # @Document(collection = "users")
    ├── dto/
    │   ├── RegisterRequest.java    # name, email, password
    │   └── UserResponse.java       # id, name, email, createdAt
    └── exception/
        ├── DuplicateUserException.java
        └── UserNotFoundException.java
```

Cada módulo de dominio usa subpaquetes por capa (`controller`, `service`, `repository`, `mapper`,
`entity`, `dto`); el paquete `exception/` de la raíz concentra el contrato de error compartido.

## Responsabilidades por capa

| Capa | Clase | Responsabilidad | No hace |
|---|---|---|---|
| Controller | `UserController` | Recibe HTTP, valida con `@Valid`, devuelve status y DTO | Reglas de negocio ni mapeo campo a campo |
| Service | `UserService` | Normaliza email, verifica unicidad, hashea contraseña, arma la entidad y consulta | Conocer HTTP |
| Repository | `UserRepository` | Persistencia en MongoDB y búsqueda por email | Reglas de negocio |
| Mapper | `UserMapper` | Convierte `User` → `UserResponse` (MapStruct) | Reglas de negocio |
| Entity | `User` | Documento persistido en la colección `users` | Exponerse por la API |
| DTO | `RegisterRequest`, `UserResponse` | Contrato HTTP de entrada y salida | Lógica de negocio |

`User` no se expone: el controller solo devuelve `UserResponse` (sin `passwordHash`).

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
y el índice único de MongoDB (`DuplicateKeyException`, también mapeada a 409).

## Endpoints

| Método | Ruta | Request | Respuesta OK | Errores |
|---|---|---|---|---|
| POST | `/api/v1/users` | `RegisterRequest` | `201` + `UserResponse` | 400 `VALIDATION_ERROR`, 400 `BAD_REQUEST`, 409 `CONFLICT`, 500 `INTERNAL_ERROR` |
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
  ← UserResponse
← 201 Created + UserResponse
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

server:
  port: ${PORT:8080}
  error:
    include-stacktrace: never
    include-message: never
```

`MONGO_URI` y `MONGO_DATABASE` son obligatorias y no tienen valor por defecto: si faltan, el arranque
falla explícitamente. No hay base embebida como fallback en ningún perfil.

Perfiles:

| Perfil | Índice único de email | Uso |
|---|---|---|
| `dev` | Se crea automáticamente | Desarrollo local |
| `docker` | Se crea automáticamente | Docker Compose |
| `test` | Se crea automáticamente | Tests de integración con Mongo embebido |
| `prod` | Desactivado; lo crea el operador | Producción |

En `prod`, Swagger UI y el endpoint de OpenAPI están deshabilitados (`springdoc.swagger-ui.enabled`
y `springdoc.api-docs.enabled` en `false`).

## Tests y calidad

| Capa | Test | Enfoque |
|---|---|---|
| Entidad | `UserTest` | `equals`/`hashCode`/`toString` sin datos sensibles |
| DTO | `RegisterRequestValidationTest` | Restricciones de Bean Validation |
| Repositorio | `UserRepositoryTest` | MongoDB embebido, búsqueda y unicidad |
| Servicio | `UserServiceTest`, `UserRegistrationIntegrationTest` | Normalización, hash, duplicado, flujo real |
| Mapper | `UserMapperTest` | `User` → `UserResponse` |
| Controller | `UserControllerTest` | `@WebMvcTest`: 201, 400, 404, 409 y contrato |
| Contrato de error | `ErrorContractHttpTest`, `GlobalExceptionHandlerTest` | Mapeo HTTP y JSON |
| Contexto | `UsersApplicationTest` | Arranque con MongoDB embebido |
| OpenAPI | `OpenApiIntegrationTest`, `OpenApiExportTest` | Publicación y export a `delivery/openapi/` |

JaCoCo exige 80 % de cobertura de líneas en `verify`. Comando de cierre:
`./mvnw -q -B verify` desde la raíz o `.\mvnw.cmd clean verify` dentro del servicio.

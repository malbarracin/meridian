---
title: Auth Service — Arquitectura (H2.1)
status: borrador
source: specs/auth/persistencia.md + project.yaml
updated: 2026-10-22T14:31:59.482Z
---

# Auth Service — Arquitectura (H2.1)

## Fuente

- Especificación funcional: `specs/auth/persistencia.md` (H2.1 — Persistencia MongoDB Auth)
- `project.yaml`: `java.group_id: com.meridian`, `java.base_package: com.meridian.users`

---

## 1. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| Carpeta | `services/auth-service` | spec `auth/persistencia.md` |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `auth-service` | spec: "servicio de autenticación (`services/auth-service`)" |
| `base_package` | `com.meridian.users` | `project.yaml java.base_package` |
| Clase principal | `UsersApplication` | `base_package` + convención `*Application` |
| Java | 21 | default (no en `project.yaml java.version`) |
| Spring Boot | 3.x última estable | default |
| Persistencia | MongoDB | spec: `spring-boot-starter-data-mongodb` |
| Puerto | 8080 | default (no en `project.yaml service.port`) |
| API versioning | **deshabilitado** | `api.versioning` ausente en `project.yaml` |

---

## 2. Árbol de paquetes

```
services/auth-service/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper/
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/meridian/users/
    │   │   ├── UsersApplication.java
    │   │   ├── config/                              # OpenApiConfig (springdoc)
    │   │   ├── exception/                           # contrato de error transversal
    │   │   │   ├── ErrorResponse.java               # record
    │   │   │   ├── FieldError.java                  # record
    │   │   │   ├── ErrorCode.java                   # enum: VALIDATION_ERROR, BAD_REQUEST, NOT_FOUND, CONFLICT, INTERNAL_ERROR
    │   │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice
    │   │   │   ├── NotFoundException.java           # base abstracta (RuntimeException)
    │   │   │   └── ConflictException.java           # base abstracta (RuntimeException)
    │   │   └── user/
    │   │       ├── controller/
    │   │       │   └── UserController.java          # @RestController @RequestMapping("/api/v1/users")
    │   │       ├── service/
    │   │       │   └── UserService.java
    │   │       ├── repository/
    │   │       │   └── UserRepository.java          # MongoRepository<User, String>
    │   │       ├── mapper/
    │   │       │   └── UserMapper.java              # @Mapper(componentModel="spring")
    │   │       ├── entity/
    │   │       │   └── User.java                    # @Document(collection="users")
    │   │       ├── dto/
    │   │       │   ├── RegisterRequest.java         # record: name, email, password
    │   │       │   └── UserResponse.java            # record: id, name, email, createdAt
    │   │       └── exception/
    │   │           └── DuplicateUserException.java  # extends ConflictException
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/meridian/users/
        │   ├── UsersApplicationTest.java            # contextLoads
        │   └── user/
        │       ├── controller/
        │       │   └── UserControllerTest.java      # @WebMvcTest
        │       ├── service/
        │       │   └── UserServiceTest.java         # @ExtendWith(MockitoExtension)
        │       └── repository/
        │           └── UserRepositoryTest.java      # @DataMongoTest
        └── resources/
            └── application.yml                     # Flapdoodle embebido o Testcontainers MongoDB
```

> **Regla de paquetes**: estructura por subpaquetes (`controller/`, `service/`, `repository/`, `mapper/`, `entity/`, `dto/`) bajo cada módulo de dominio. Prohibido paquete plano (`user.UserController` sin subcapa).

---

## 3. Entidad `User`

Documento MongoDB en la colección `users`.

```
com.meridian.users.user.entity.User
```

| Campo | Tipo Java | Anotación MongoDB | Notas |
|-------|-----------|-------------------|-------|
| `id` | `String` | `@Id` | ObjectId generado por MongoDB |
| `name` | `String` | — | nombre del usuario |
| `email` | `String` | `@Indexed(unique=true)` | almacenado en minúsculas sin espacios (normalizado antes de persistir) |
| `passwordHash` | `String` | — | NUNCA el password en texto plano |
| `createdAt` | `Instant` | — | asignado en `@PersistenceConstructor` o en el service |
| `updatedAt` | `Instant` | — | actualizado en cada escritura |

- `@Document(collection = "users")` en la clase.
- `@Getter @Setter` (Lombok; no `@Data` para evitar problemas con `equals/hashCode` en entidades).
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` con `@EqualsAndHashCode.Include` en `id`.
- Índice único en `email`: creado via `@Indexed(unique = true)` con `spring.data.mongodb.auto-index-creation: true` (solo en dev/test) **o** via `@CompoundIndex` / script de inicialización en producción. El dev elige la vía; en tests se verifica el rechazo.

---

## 4. Repositorio

```
com.meridian.users.user.repository.UserRepository
```

```java
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
```

- Extiende `MongoRepository<User, String>`.
- Operación obligatoria: `findByEmail(String email)` → `Optional<User>`.
- El service la invoca con el email ya normalizado (trim + lowercase).

---

## 5. DTOs

### `RegisterRequest` (request)
```
com.meridian.users.user.dto.RegisterRequest
```
```java
public record RegisterRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) {}
```

### `UserResponse` (response)
```
com.meridian.users.user.dto.UserResponse
```
```java
public record UserResponse(
    String id,
    String name,
    String email,
    Instant createdAt
) {}
```

> `passwordHash` **nunca** aparece en ningún DTO de respuesta.

---

## 6. Mapper

```
com.meridian.users.user.mapper.UserMapper
```

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
    // User toEntity(RegisterRequest req) no se mapea directamente:
    // el service construye la entidad (necesita hashear el password antes)
}
```

El service construye el `User` directamente (no via mapper) porque debe hashear el password; el mapper solo convierte `User → UserResponse`.

---

## 7. Endpoints

No hay `api.versioning.enabled` en `project.yaml` → todos los endpoints bajo `/api/v1/`.

### POST `/api/v1/users`  —  Registrar usuario

| | |
|---|---|
| **Controller** | `UserController.registerUser(@Valid @RequestBody RegisterRequest req)` |
| **Request** | `RegisterRequest` (name, email, password) |
| **Response exitosa** | `201 Created` + `UserResponse` (id, name, email, createdAt) |
| **Errores** | ver tabla abajo |

**Tabla de errores del endpoint:**

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Campos inválidos (`@Valid`) | 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException` |
| Email ya registrado | 409 | `CONFLICT` | `DuplicateUserException extends ConflictException` |
| Error inesperado | 500 | `INTERNAL_ERROR` | `Exception` |

### GET `/api/v1/users/{id}`  —  Obtener usuario por id *(alcance mínimo para tests; puede expandirse en features siguientes)*

| | |
|---|---|
| **Controller** | `UserController.getUser(@PathVariable String id)` |
| **Response exitosa** | `200 OK` + `UserResponse` |
| **Errores** | |

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Usuario no encontrado | 404 | `NOT_FOUND` | `UserNotFoundException extends NotFoundException` |

> **Excepciones de dominio** a crear:
> - `DuplicateUserException extends ConflictException` — en `user/exception/`
> - `UserNotFoundException extends NotFoundException` — en `user/exception/`

---

## 8. Flujo de datos

```
POST /api/v1/users
  → UserController.registerUser(@Valid RegisterRequest)
      → UserService.register(RegisterRequest)
          normaliza email (trim + lowercase)
          verifica unicidad (UserRepository.findByEmail)  → lanza DuplicateUserException si existe
          hashea password (BCrypt o similar)
          construye User (email normalizado, passwordHash, createdAt, updatedAt = now())
          UserRepository.save(User)  → lanza DuplicateKeyException si índice falla (segunda defensa)
          → UserMapper.toResponse(User)
      ← UserResponse
  ← 201 Created + UserResponse
```

> La normalización del email ocurre en `UserService`, antes de cualquier operación de repositorio.  
> La `DuplicateKeyException` de MongoDB (si llega a propagarse) debe ser interceptada en `GlobalExceptionHandler` y mapeada a `409 CONFLICT` con `code: CONFLICT`.

---

## 9. Configuración (`application.yml`)

```yaml
spring:
  application:
    name: auth-service
  data:
    mongodb:
      uri: ${MONGO_URI}
      database: ${MONGO_DATABASE}
      auto-index-creation: false   # los índices se crean por script/init en producción

server:
  port: ${PORT:8080}
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
```

**Variables de entorno obligatorias** (sin fallback silencioso):

| Variable | Descripción |
|----------|-------------|
| `MONGO_URI` | URI completa de conexión a MongoDB (ej. `mongodb://localhost:27017`) |
| `MONGO_DATABASE` | Nombre de la base de datos (ej. `meridian`) |

> No hay valor por defecto para `MONGO_URI` ni `MONGO_DATABASE`: si faltan, Spring falla al arrancar con error explícito. Esto cumple el requisito "sin fallback in-memory silencioso".

**`application.yml` para tests** (`src/test/resources/application.yml`):

```yaml
spring:
  application:
    name: auth-service
  data:
    mongodb:
      auto-index-creation: true   # permite crear índices en el Mongo embebido de tests
```

> Los tests usan **Flapdoodle Embedded MongoDB** (`de.flapdoodle.embed:de.flapdoodle.embed.mongo`) o **Testcontainers** (`org.testcontainers:mongodb`). El dev elige; ambos son válidos.

---

## 10. `pom.xml` — dependencias clave

La plantilla base es `pom.xml.md` del pack java, con los siguientes ajustes:

| Acción | Dependencia |
|--------|-------------|
| **Agregar** | `spring-boot-starter-data-mongodb` (sin versión — BOM) |
| **Reemplazar** `spring-boot-starter-data-jpa` + driver PostgreSQL | — (no aplica: persistencia es MongoDB) |
| **Agregar** (tests) | `de.flapdoodle.embed:de.flapdoodle.embed.mongo` (scope test) **o** `org.testcontainers:mongodb` (scope test) |
| **Quitar** | `h2` (no se usa como fallback; los tests van contra Mongo embebido) |
| Mantener | `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, Lombok, MapStruct, springdoc, JaCoCo |

> Versión de Flapdoodle: compatible con Spring Boot 3.x (el BOM la resuelve vía `spring-boot-starter-test` en versiones recientes; si no está en el BOM, usar la versión que lista la release notes de Spring Boot 3).

---

## 11. Estrategia de tests

| Capa | Clase | Tipo | Qué prueba |
|------|-------|------|------------|
| Controller | `UserControllerTest` | `@WebMvcTest` + `MockMvc` | POST devuelve 201 y body correcto; 400 con campo inválido; 409 email duplicado (mock del service). Verifica `$.code`, `$.details`, `$.timestamp` en errores. |
| Service | `UserServiceTest` | `@ExtendWith(MockitoExtension)` | normalización email; hasheo password; lanza `DuplicateUserException` si email existe; construye entidad correctamente. |
| Repository | `UserRepositoryTest` | `@DataMongoTest` | `findByEmail` encuentra/no encuentra; unicidad de email (segundo save del mismo email lanza `DuplicateKeyException`). |
| Integración | `UsersApplicationTest` | `@SpringBootTest` | `contextLoads` con variables Mongo presentes (via `@DynamicPropertySource` de Flapdoodle/Testcontainers). |

JaCoCo: umbral mínimo **80%** line coverage. Exclusiones sugeridas: `UsersApplication`, clases de `config/` sin lógica.

---

## 12. Riesgos y decisiones pendientes para el Dev

| # | Riesgo / decisión | Recomendación |
|---|-------------------|---------------|
| 1 | **`DuplicateKeyException` de MongoDB** puede llegar al handler si el índice la atrapa antes que el service | Agregar handler explícito en `GlobalExceptionHandler` para `org.springframework.dao.DuplicateKeyException` → 409 CONFLICT |
| 2 | **`auto-index-creation`** en producción | Desactivado en producción; crear el índice único en `email` via script de init o migration (Mongock). En tests activarlo para el Mongo embebido. |
| 3 | **Hash del password** | La spec no nombra librería. Usar `BCryptPasswordEncoder` de Spring Security (solo el encoder, sin levantar toda la cadena de seguridad HTTP) o `spring-security-crypto` standalone. El dev confirma. |
| 4 | **Flapdoodle vs Testcontainers** | Flapdoodle es más ligero para CI sin Docker; Testcontainers es más fiel a producción. El dev elige según el entorno de CI. |
| 5 | **`UserNotFoundException`** para el GET por id | Alcance mínimo del endpoint GET definido acá; si la spec lo excluye del H2.1, el dev puede diferirlo a una feature posterior. |

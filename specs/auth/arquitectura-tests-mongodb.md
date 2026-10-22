---
title: "Auth Service — Arquitectura H2.3: Tests de integración con MongoDB"
status: borrador
source: specs/auth/tests-auth-mongodb.md +
  specs/auth/arquitectura-registro-seguro.md + código existente en
  services/auth-service
updated: 2026-10-22T17:59:34.027Z
---

# Auth Service — Arquitectura H2.3: Tests de integración con MongoDB

## Fuente

- Especificación funcional: `specs/auth/tests-auth-mongodb.md` (H2.3 — Tests Auth con MongoDB)
- Especificación previa: `specs/auth/arquitectura-registro-seguro.md` (H2.2 — Registro seguro + JWT)
- `project.yaml`: `java.group_id: com.meridian`, `java.base_package: com.meridian.users`
- Código existente: `services/auth-service` (rama `opensoma/20261022-9f2c`)

---

## 1. Contexto: qué ya existe (H2.1 + H2.2)

Los hitos H2.1 y H2.2 entregaron:

| Clase | Estado |
|-------|--------|
| `UsersApplication` | ✅ intacta |
| `User` (entidad) | ✅ intacta |
| `UserRepository` | ✅ intacta |
| `RegisterRequest`, `UserResponse`, `AuthResponse` (DTOs) | ✅ intactos |
| `UserMapper` | ✅ intacto |
| `UserService` (register + getById) | ✅ intacto — **no se modifica lógica de registro** |
| `JwtService` | ✅ intacto |
| `JwtConfig` | ✅ intacto |
| `GlobalExceptionHandler`, `SecurityConfig`, `OpenApiConfig` | ✅ intactos |
| `UserController` (POST /api/v1/users + GET /api/v1/users/{id}) | ✅ intacto |
| Tests unitarios y @WebMvcTest existentes | ✅ intactos — no se tocan |

Lo que H2.3 agrega:

| Qué | Tipo |
|-----|------|
| `LoginRequest` (DTO) | NUEVO |
| `LoginResponse` (alias de `AuthResponse`) | DECISIÓN: reusar `AuthResponse` |
| `AuthController` (POST /api/v1/auth/login) | NUEVO |
| `UserService.login()` | NUEVO método |
| `InvalidCredentialsException` | NUEVO |
| Endpoint GET /api/v1/users/me (en `UserController`) | NUEVO método |
| Tests de integración con Testcontainers | NUEVO |
| Dependencia `testcontainers` en pom.xml | NUEVO |

---

## 2. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| `base_package` | `com.meridian.users` | `project.yaml java.base_package` |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `auth-service` | existente |
| Persistencia | MongoDB + Testcontainers (tests) | H2.3 |
| API versioning | **deshabilitado** | `api.versioning` ausente en `project.yaml` |

---

## 3. Nuevos endpoints que H2.3 requiere testar

La spec H2.3 exige tests de login y `/me`, que implican endpoints aún no implementados. Ambos se crean en H2.3.

### 3.1. POST `/api/v1/auth/login` — Autenticación

| | |
|---|---|
| **Controller** | `AuthController.login(@Valid @RequestBody LoginRequest req)` |
| **Request** | `LoginRequest` (email, password) |
| **Response exitosa** | `200 OK` + `AuthResponse` (user + token) |
| **Errores** | ver tabla abajo |

**Tabla de errores:**

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Campos inválidos (`@Valid`) | 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException` |
| JSON mal formado | 400 | `BAD_REQUEST` | `HttpMessageNotReadableException` |
| Email no encontrado o password incorrecto | 401 | `UNAUTHORIZED` | `InvalidCredentialsException extends UnauthorizedException` |
| Error inesperado | 500 | `INTERNAL_ERROR` | `Exception` |

> **Seguridad**: responder siempre `401 UNAUTHORIZED` con el mismo mensaje genérico (`"Credenciales inválidas"`) para email inexistente o password incorrecto, sin distinguir cuál de los dos falló (evitar user enumeration).

### 3.2. GET `/api/v1/users/me` — Perfil del usuario autenticado

| | |
|---|---|
| **Controller** | `UserController.getMe(@RequestHeader("Authorization") String authHeader)` |
| **Request** | Header `Authorization: Bearer <token>` |
| **Response exitosa** | `200 OK` + `UserResponse` (id, name, email, createdAt — sin passwordHash) |
| **Errores** | ver tabla abajo |

**Tabla de errores:**

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Token ausente o malformado | 401 | `UNAUTHORIZED` | `InvalidCredentialsException` |
| Token expirado o firma inválida | 401 | `UNAUTHORIZED` | `InvalidCredentialsException` |
| Usuario del token no encontrado en BD | 404 | `NOT_FOUND` | `UserNotFoundException` |
| Error inesperado | 500 | `INTERNAL_ERROR` | `Exception` |

> La implementación de `/me` extrae el `sub` (userId) del JWT vía `JwtService`, sin necesidad de Spring Security completo. `JwtService.getSubject(token)` se agrega como método de conveniencia.

---

## 4. Árbol de archivos — cambios en H2.3

Solo se listan los archivos nuevos o modificados respecto a H2.2:

```
services/auth-service/src/
├── main/
│   ├── java/com/meridian/users/
│   │   ├── exception/
│   │   │   ├── UnauthorizedException.java          [NUEVO]  extends RuntimeException (base abstracta)
│   │   │   └── GlobalExceptionHandler.java         [MODIFICAR] agregar handler 401
│   │   ├── exception/ErrorCode.java                [MODIFICAR] agregar UNAUTHORIZED
│   │   └── user/
│   │       ├── controller/
│   │       │   ├── AuthController.java             [NUEVO]  POST /api/v1/auth/login
│   │       │   └── UserController.java             [MODIFICAR] agregar GET /me
│   │       ├── service/
│   │       │   ├── UserService.java                [MODIFICAR] agregar login() y getSubjectFromToken()
│   │       │   └── JwtService.java                 [MODIFICAR] agregar getSubject(token)
│   │       ├── dto/
│   │       │   └── LoginRequest.java               [NUEVO]  record: email, password
│   │       └── exception/
│   │           └── InvalidCredentialsException.java [NUEVO]  extends UnauthorizedException
└── test/
    └── java/com/meridian/users/
        └── user/
            └── integration/
                ├── RegisterIntegrationTest.java    [NUEVO]  @SpringBootTest + Testcontainers
                ├── LoginIntegrationTest.java       [NUEVO]  @SpringBootTest + Testcontainers
                └── ProfileIntegrationTest.java     [NUEVO]  @SpringBootTest + Testcontainers
```

> Los tests de integración van en el subpaquete `user/integration/` bajo el paquete de tests. Los tests unitarios y @WebMvcTest existentes **no se tocan**.

---

## 5. Nuevos DTOs

### `LoginRequest`

```
com.meridian.users.user.dto.LoginRequest
```

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
```

### Reutilización de `AuthResponse`

El endpoint de login devuelve el mismo `AuthResponse` que el de registro (`{ user: UserResponse, token: String }`). No se crea un DTO separado.

---

## 6. Nuevas excepciones de dominio

### `UnauthorizedException` (base — en `exception/`)

```
com.meridian.users.exception.UnauthorizedException
```

```java
public abstract class UnauthorizedException extends RuntimeException {
    protected UnauthorizedException(String message) { super(message); }
}
```

### `InvalidCredentialsException` (en `user/exception/`)

```
com.meridian.users.user.exception.InvalidCredentialsException
```

```java
public class InvalidCredentialsException extends UnauthorizedException {
    public InvalidCredentialsException() { super("Credenciales inválidas"); }
}
```

---

## 7. `ErrorCode` — agregar `UNAUTHORIZED`

```java
public enum ErrorCode {
    VALIDATION_ERROR,
    BAD_REQUEST,
    NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,   // NUEVO
    INTERNAL_ERROR
}
```

---

## 8. `GlobalExceptionHandler` — agregar handler 401

Agregar en la clase existente:

```java
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
    return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, ex.getMessage(), List.of());
}
```

---

## 9. `JwtService` — agregar `getSubject(token)`

Método de conveniencia para extraer el `sub` del JWT sin lanzar excepción: devuelve `Optional<String>`.

```java
/**
 * Extrae el subject (userId) si el token es válido.
 * @return Optional con el subject, o vacío si el token es inválido o expirado
 */
public Optional<String> getSubject(String token) {
    try {
        return Optional.of(Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    } catch (JwtException | IllegalArgumentException e) {
        return Optional.empty();
    }
}
```

---

## 10. `UserService` — agregar `login()` y `getByEmail()`

```java
/**
 * Autentica un usuario con sus credenciales.
 * @param request email y password en texto plano
 * @return datos públicos del usuario y JWT de acceso
 * @throws InvalidCredentialsException si el email no existe o el password no coincide
 */
public AuthResponse login(LoginRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new InvalidCredentialsException();
    }
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(userMapper.toResponse(user), token);
}

/**
 * Obtiene el perfil del usuario a partir del userId extraído del JWT.
 * @param userId identificador del usuario (sub del token)
 * @return datos públicos del usuario
 * @throws UserNotFoundException si el userId no existe en BD
 */
public UserResponse getById(String userId) {
    // ya existe — sin cambios
}
```

---

## 11. `AuthController`

```
com.meridian.users.user.controller.AuthController
```

```java
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y gestión de tokens de acceso")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
```

---

## 12. `UserController` — agregar GET `/me`

Agregar en el controller existente:

```java
/**
 * Devuelve el perfil del usuario autenticado según el JWT del header Authorization.
 */
@GetMapping("/me")
public ResponseEntity<UserResponse> getMe(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new InvalidCredentialsException();
    }
    String token = authHeader.substring(7);
    String userId = jwtService.getSubject(token)
            .orElseThrow(InvalidCredentialsException::new);
    return ResponseEntity.ok(userService.getById(userId));
}
```

> `UserController` necesita inyectar también `JwtService` para delegar la extracción del subject. La dependencia `JwtService` se agrega al constructor vía `@RequiredArgsConstructor`.

---

## 13. Configuración: Testcontainers en lugar de Flapdoodle (solo tests de integración)

Los tests de integración de H2.3 usan Testcontainers. Los tests unitarios y @WebMvcTest existentes siguen usando Flapdoodle (sin cambio en `application.yml` de test).

### Dependencias a agregar en `pom.xml`

```xml
<!-- Testcontainers BOM (scope test) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Las versiones las provee el BOM de `spring-boot-starter-parent` (Spring Boot 3.x incluye el BOM de Testcontainers). No agregar versiones manuales.

> **Coexistencia Flapdoodle + Testcontainers**: los tests de integración nuevos no usan `@DataMongoTest` ni `@SpringBootTest` sin `@DynamicPropertySource` propio — configuran su propia URI de Testcontainers. Los tests existentes con Flapdoodle no se modifican y coexisten en el mismo build.

---

## 14. Clase base para tests de integración con Testcontainers

Crear una clase base `AbstractMongoIntegrationTest` en el paquete de integración para reutilizar el contenedor entre tests (Singleton pattern de Testcontainers):

```
com.meridian.users.user.integration.AbstractMongoIntegrationTest
```

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractMongoIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(false);

    @DynamicPropertySource
    static void setMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected MongoTemplate mongoTemplate;
}
```

Los tests concretos extienden esta clase. Cada test limpia la colección en `@BeforeEach`.

---

## 15. Tests de integración — diseño detallado

### 15.1. `RegisterIntegrationTest`

Cubre requisitos: "Registro persiste usuario", "Email duplicado → 409", "Password almacenado como hash".

| Test | Escenario | Verificación |
|------|-----------|--------------|
| `registroPersisteTodosLosCampos` | POST /api/v1/users con datos válidos | 201; documento en MongoDB tiene id, name, email, passwordHash, createdAt, updatedAt; passwordHash empieza con `$2a$` o `$2b$`; campo password no aparece en documento |
| `emailDuplicadoExacto409` | POST con mismo email exacto | 409; `$.code = CONFLICT`; un solo documento en MongoDB |
| `emailDuplicadoMayusculas409` | POST con email en mayúsculas tras registro con minúsculas | 409; `$.code = CONFLICT`; un solo documento |
| `passwordGuardadoComoHash` | Leer documento de MongoDB directamente tras registro | `passwordHash` comienza con `$2a$` o `$2b$`; texto plano no aparece en ningún campo |

### 15.2. `LoginIntegrationTest`

Cubre requisitos: "Login correcto tras registro", "Login incorrecto → 401".

| Test | Escenario | Verificación |
|------|-----------|--------------|
| `loginCorrecto200ConJwt` | Registrar + POST /api/v1/auth/login con mismas credenciales | 200; `$.token` no vacío; JWT decodificable con sub = userId |
| `loginPasswordIncorrecto401` | POST login con password diferente al registrado | 401; `$.code = UNAUTHORIZED` |
| `loginEmailInexistente401` | POST login con email no registrado | 401; `$.code = UNAUTHORIZED` |

### 15.3. `ProfileIntegrationTest`

Cubre requisitos: "Test de /me", "Seed idempotente", "Persistencia tras reinicio de conexión".

| Test | Escenario | Verificación |
|------|-----------|--------------|
| `meLeePerfilPersistido` | Registrar + GET /api/v1/users/me con Bearer token | 200; response contiene id, name, email; NO contiene passwordHash |
| `meConTokenInvalido401` | GET /api/v1/users/me con token alterado | 401; `$.code = UNAUTHORIZED` |
| `meConTokenAusente401` | GET /api/v1/users/me sin header Authorization | 401; `$.code = UNAUTHORIZED` |
| `seedIdempotente` | Insertar mismo documento dos veces con upsert | No lanza excepción; un solo documento en colección |
| `persistenciaDisponibleTrasReconexion` | Insertar documento; pausar/reanudar; consultar | Documento sigue disponible; read funciona tras reconexión |

> **Mecanismo de "reinicio de conexión"**: usar `mongoTemplate.getDb().runCommand(Document("connectionStatus", 1))` para verificar conectividad, luego hacer una escritura y una lectura. Un cierre/apertura de socket real depende del driver; se usa el `MongoTemplate` con retry natural del driver.

---

## 16. Gate JaCoCo

La spec H2.3 indica LINE ≥ 70%. El stack pack exige 80%. **Decisión del arquitecto**: mantener el umbral en **80%** conforme al stack pack, que es más estricto y ya estaba configurado. Los tests de integración nuevos aportan cobertura sobre controller, service y repository, lo que debería cubrir ese umbral con holgura.

Si al ejecutar los tests el umbral 80% no se alcanza (inesperado), el dev puede proponer ajuste documentado, pero no bajar por debajo de 70% sin OK explícito.

---

## 17. Flujo de datos — nuevos endpoints

### POST `/api/v1/auth/login`

```
POST /api/v1/auth/login
  → AuthController.login(@Valid LoginRequest)
      → UserService.login(LoginRequest)
          normaliza email (trim + lowercase)
          UserRepository.findByEmail(email)  → InvalidCredentialsException si no existe
          passwordEncoder.matches(password, hash)  → InvalidCredentialsException si no coincide
          JwtService.generateToken(userId, email)  → token
          UserMapper.toResponse(user)  → UserResponse
          new AuthResponse(userResponse, token)
      ← AuthResponse
  ← 200 OK + AuthResponse { user: { id, name, email, createdAt }, token: "eyJ..." }
```

### GET `/api/v1/users/me`

```
GET /api/v1/users/me
  Header: Authorization: Bearer <token>
  → UserController.getMe(authHeader)
      extrae token del header  → InvalidCredentialsException si ausente o malformado
      JwtService.getSubject(token)  → Optional<String>  → InvalidCredentialsException si vacío
      UserService.getById(userId)  → UserResponse  → UserNotFoundException si no existe
  ← 200 OK + UserResponse { id, name, email, createdAt }
```

---

## 18. Riesgos y decisiones pendientes para el Dev

| # | Riesgo / decisión | Recomendación |
|---|-------------------|---------------|
| 1 | **Coexistencia Flapdoodle + Testcontainers** | Testcontainers requiere Docker disponible en CI. Si CI no tiene Docker, los tests `@Testcontainers` fallarán. Dev confirma que CI tiene Docker o usa `assumeTrue(DockerClientFactory.instance().isDockerAvailable())`. |
| 2 | **`UserController` agrega `JwtService` como dependencia** | `@RequiredArgsConstructor` genera el constructor; los `@WebMvcTest` existentes deben agregar `@MockBean JwtService` para no romper. Verificar `UserControllerTest` tras el cambio. |
| 3 | **Patrón Singleton de Testcontainers** | La clase base `AbstractMongoIntegrationTest` con `@Container static` reutiliza el contenedor entre clases que la extiendan en el mismo ClassLoader. Si se necesita aislamiento total, usar `@DirtiesContext`. |
| 4 | **`mongo:7.0` en Testcontainers** | Usar la misma versión que la configuración de Flapdoodle (`7.0.14`). Si el entorno no tiene esa imagen, Testcontainers la descarga automáticamente. |
| 5 | **Endpoint `/me` sin Spring Security completo** | La implementación extrae el userId del JWT manualmente vía `JwtService.getSubject()`. No requiere filtros de seguridad ni `SecurityContextHolder`. Es suficiente para H2.3; si se agrega Spring Security completo en un hito futuro, el endpoint se refactoriza. |
| 6 | **`InvalidCredentialsException` con mensaje genérico** | No distinguir "email no existe" de "password incorrecto" en el mensaje de respuesta (evitar user enumeration). El log interno sí puede distinguirlos. |
| 7 | **`LoginRequest.password` — sin `@Size(min=8)`** | En login no tiene sentido validar longitud mínima de password (ya fue validado al registrar). Solo `@NotBlank`. |
| 8 | **Test "persistencia tras reinicio de conexión"** | Implementar como verificación de conectividad + write + read usando `MongoTemplate`. Un reinicio real del contenedor (`.stop()` / `.start()`) puede ser costoso; se acepta una verificación de reconexión leve con el Mongo client. |

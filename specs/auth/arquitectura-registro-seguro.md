---
title: "Auth Service — Arquitectura H2.2: Registro seguro + JWT"
status: borrador
source: specs/auth/registro-seguro.md + specs/auth/arquitectura.md +
  project.yaml + código existente en services/auth-service
updated: 2026-10-22T16:16:19.491Z
---

# Auth Service — Arquitectura H2.2: Registro seguro + JWT

## Fuente

- Especificación funcional: `specs/auth/registro-seguro.md` (H2.2 — Registro seguro + hashing)
- Especificación previa: `specs/auth/arquitectura.md` (H2.1 — Persistencia MongoDB)
- `project.yaml`: `java.group_id: com.meridian`, `java.base_package: com.meridian.users`
- Código existente: `services/auth-service` (rama `opensoma/20261022-7bab`)

---

## 1. Contexto: qué ya existe (H2.1)

H2.1 entregó el scaffold completo del servicio. Las siguientes clases ya están implementadas y **no se modifican** salvo lo indicado:

| Clase | Estado |
|-------|--------|
| `UsersApplication` | ✅ intacta |
| `User` (entidad) | ✅ intacta |
| `UserRepository` | ✅ intacta |
| `RegisterRequest` (DTO) | ✅ intacta |
| `UserResponse` (DTO) | ✅ intacta — sigue usándose internamente |
| `UserMapper` | ✅ intacto — sigue mapeando `User → UserResponse` |
| `UserService` | ⚠️ **modificar**: añadir generación de JWT y devolver `AuthResponse` |
| `UserController` | ⚠️ **modificar**: endpoint POST devuelve `AuthResponse` en vez de `UserResponse` |
| `GlobalExceptionHandler` | ✅ intacto |
| `SecurityConfig` | ✅ intacto (`PasswordEncoder` ya configurado) |
| `OpenApiConfig` | ⚠️ **modificar**: ejemplos del endpoint POST actualizados para `AuthResponse` |

---

## 2. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| `base_package` | `com.meridian.users` | `project.yaml java.base_package` |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `auth-service` | existente |
| Persistencia | MongoDB | H2.1 |
| API versioning | **deshabilitado** | `api.versioning` ausente en `project.yaml` |
| Endpoint de registro | `POST /api/v1/users` | H2.1 (sin cambio de ruta) |

---

## 3. Nuevo DTO: `AuthResponse`

```
com.meridian.users.user.dto.AuthResponse
```

```java
@Schema(description = "Respuesta de registro exitoso: perfil público del usuario y JWT de acceso.")
public record AuthResponse(
    @Schema(description = "Datos públicos del usuario recién creado.")
    UserResponse user,

    @Schema(description = "JWT firmado (HS256) listo para usar en cabeceras Authorization Bearer.",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    String token
) {}
```

- `passwordHash` **nunca** aparece ni en `UserResponse` ni en `AuthResponse`.
- `UserResponse` se mantiene igual que en H2.1 (lo usa el GET `/api/v1/users/{id}` sin cambio).

---

## 4. Árbol de archivos — cambios en H2.2

Solo se listan los archivos nuevos o modificados:

```
services/auth-service/src/
├── main/
│   ├── java/com/meridian/users/
│   │   ├── config/
│   │   │   └── JwtConfig.java              [NUEVO]  @ConfigurationProperties("jwt")
│   │   ├── user/
│   │   │   ├── dto/
│   │   │   │   └── AuthResponse.java       [NUEVO]  record: user + token
│   │   │   └── service/
│   │   │       ├── UserService.java        [MODIFICAR] register() devuelve AuthResponse
│   │   │       └── JwtService.java         [NUEVO]  genera y verifica JWT (HS256, JJWT)
│   └── resources/
│       └── application.yml                 [MODIFICAR] añadir bloque jwt.*
└── test/
    └── java/com/meridian/users/
        └── user/
            ├── controller/
            │   └── UserControllerTest.java [MODIFICAR] assertions sobre AuthResponse + token
            └── service/
                ├── UserServiceTest.java    [MODIFICAR] verifica token en resultado
                └── JwtServiceTest.java     [NUEVO]  genera/valida token; verifica claims
```

---

## 5. `JwtService`

```
com.meridian.users.user.service.JwtService
```

Responsabilidades:
- Generar un JWT firmado (HS256) con los claims mínimos: `sub` (userId), `email`, `iat`, `exp`.
- Proveer método de validación para uso futuro (login).

```java
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    public JwtService(JwtConfig jwtConfig) { this.jwtConfig = jwtConfig; }

    /** Genera JWT con sub=userId, email en claims, exp según configuración. */
    public String generateToken(String userId, String email) { ... }

    /** Verifica la firma y la expiración del token. */
    public boolean isValid(String token) { ... }
}
```

Claims del JWT:

| Claim | Valor |
|-------|-------|
| `sub` | `userId` (id de MongoDB) |
| `email` | email normalizado del usuario |
| `iat` | `Instant.now()` |
| `exp` | `Instant.now() + jwtConfig.expirationMs()` |

Algoritmo: **HS256**. La clave secreta viene de la variable de entorno `JWT_SECRET` (mínimo 32 caracteres).

---

## 6. `JwtConfig`

```
com.meridian.users.config.JwtConfig
```

```java
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(String secret, long expirationMs) {}
```

Requiere `@EnableConfigurationProperties(JwtConfig.class)` en la clase principal o en una `@Configuration`.

Valores en `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}   # 24h por defecto
```

Variables de entorno:

| Variable | Descripción | Obligatoria |
|----------|-------------|-------------|
| `JWT_SECRET` | Clave de firma HMAC-SHA256 (≥ 32 caracteres) | **Sí** |
| `JWT_EXPIRATION_MS` | Expiración del token en milisegundos | No (default 86400000 = 24h) |

---

## 7. Flujo de datos — POST `/api/v1/users` (actualizado H2.2)

```
POST /api/v1/users
  → UserController.registerUser(@Valid RegisterRequest)
      → UserService.register(RegisterRequest) → devuelve AuthResponse
          normaliza email (trim + lowercase)
          verifica unicidad (UserRepository.findByEmail)  → DuplicateUserException si existe
          hashea password (BCryptPasswordEncoder)
          construye User (email normalizado, passwordHash, createdAt, updatedAt = now())
          UserRepository.save(User)                       → DuplicateKeyException (segunda defensa → 409)
          UserMapper.toResponse(User)                     → UserResponse (sin passwordHash)
          JwtService.generateToken(user.id, user.email)  → token String
          new AuthResponse(userResponse, token)
      ← AuthResponse
  ← 201 Created + AuthResponse { user: { id, name, email, createdAt }, token: "eyJ..." }
```

---

## 8. Endpoint actualizado

### POST `/api/v1/users`  —  Registrar usuario

| | |
|---|---|
| **Controller** | `UserController.registerUser(@Valid @RequestBody RegisterRequest req)` |
| **Request** | `RegisterRequest` (name, email, password) — sin cambio |
| **Response exitosa** | `201 Created` + **`AuthResponse`** (user + token) |
| **Errores** | sin cambio — mismos códigos de H2.1 |

**Tabla de errores (sin cambio):**

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Campos inválidos (`@Valid`) | 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException` |
| JSON mal formado | 400 | `BAD_REQUEST` | `HttpMessageNotReadableException` |
| Email ya registrado (service) | 409 | `CONFLICT` | `DuplicateUserException extends ConflictException` |
| Email ya registrado (índice Mongo) | 409 | `CONFLICT` | `DuplicateKeyException` (handler ya existe) |
| Error inesperado | 500 | `INTERNAL_ERROR` | `Exception` |

### GET `/api/v1/users/{id}` — sin cambio

Devuelve `UserResponse` (sin token). No se toca.

---

## 9. Dependencia nueva en `pom.xml`

Agregar JJWT (última versión estable de la línea 0.12.x):

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

Propiedad en `<properties>`:

```xml
<jjwt.version>0.12.6</jjwt.version>
```

> JJWT 0.12.x es la versión estable actual que soporta Java 21 y Spring Boot 3.x sin configuración adicional.
> `jjwt-api` en compile, `jjwt-impl` y `jjwt-jackson` en runtime (patrón recomendado por el proyecto JJWT).

---

## 10. `application.yml` — cambios

Agregar bajo la configuración existente:

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

`JWT_SECRET` es obligatorio desde el entorno en toda ejecucion del servicio, sin valor por defecto.
No guardar secretos reales en el repositorio ni utilizar fixtures de tests como claves de despliegue.
La clave debe generarse aleatoriamente, ser privada y distinta por entorno, con al menos 32 bytes.
Una clave ausente, en blanco o insuficiente debe impedir el arranque antes de firmar o verificar tokens.

`src/test/resources/application.yml` — fixture publico exclusivo para tests automatizados, sin uso en despliegues:

```yaml
jwt:
  secret: test-secret-key-for-unit-tests-only-32chars
  expiration-ms: 3600000
```

---

## 11. Estrategia de tests

| Capa | Clase | Tipo | Qué verifica |
|------|-------|------|-------------|
| Service | `JwtServiceTest` | `@ExtendWith(MockitoExtension)` | generateToken devuelve string no vacío; isValid true para token propio; isValid false para token expirado o alterado; claims sub y email correctos. |
| Service | `UserServiceTest` | `@ExtendWith(MockitoExtension)` | register() devuelve `AuthResponse` con `user` no nulo y `token` no vacío; mock de `JwtService`. |
| Controller | `UserControllerTest` | `@WebMvcTest` + `MockMvc` | POST devuelve 201 y `$.token` existe y no está vacío; `$.user.id`, `$.user.email` presentes; `$.user.passwordHash` ausente; 400 y 409 sin cambio. |
| Integración | `UsersApplicationTest` | `@SpringBootTest` | contextLoads con JWT_SECRET y MONGO_URI presentes (via `@DynamicPropertySource`). |

JaCoCo: umbral mínimo **80%** line coverage (sin cambio). `JwtConfig` y `AuthResponse` (record puro) pueden excluirse si no tienen lógica propia.

---

## 12. Riesgos y decisiones pendientes para el Dev

| # | Riesgo / decisión | Recomendación |
|---|-------------------|---------------|
| 1 | **`JWT_SECRET` ausente en tests** | Definirlo en `src/test/resources/application.yml` como valor fijo (sin lógica real); no usar `@MockBean` de `JwtService` en el test de integración. |
| 2 | **Longitud mínima de `JWT_SECRET`** | JJWT 0.12.x exige mínimo 256 bits (32 bytes) para HMAC-SHA256. Validar al crear la configuracion y preparar la clave en el constructor de `JwtService`; rechazar configuraciones inseguras antes de operar con tokens. Documentar no reemplaza esta validacion. |
| 3 | **`@EnableConfigurationProperties`** | Agregar en `UsersApplication` o en `JwtConfig` propia `@Configuration`; el dev elige la forma más limpia para el proyecto. |
| 4 | **`OpenApiConfig` — ejemplos del POST** | Actualizar los ejemplos de respuesta 201 para mostrar `AuthResponse` con `user` y `token`; los errores 400/409/500 no cambian. |
| 5 | **`UserResponse` en `AuthResponse`** | `AuthResponse.user()` es un `UserResponse` record anidado. Jackson serializa correctamente records anidados sin configuración adicional en Spring Boot 3.x. |
| 6 | **`UserController` — `Location` header** | El header `Location` del 201 sigue apuntando a `/api/v1/users/{id}` (el recurso creado), sin cambio. |

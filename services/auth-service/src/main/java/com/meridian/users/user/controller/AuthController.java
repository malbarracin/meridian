package com.meridian.users.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.meridian.users.exception.ErrorResponse;
import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.LoginRequest;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.exception.InvalidCredentialsException;
import com.meridian.users.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * API de autenticación mediante email y contraseña.
 * <p>
 * Expone el registro ({@code POST /api/v1/auth/register}), el inicio de sesión
 * ({@code POST /api/v1/auth/login}), el perfil autenticado ({@code GET /api/v1/auth/me}) y el cierre
 * de sesión del cliente ({@code POST /api/v1/auth/logout}). El registro y el login devuelven
 * {@code accessToken}, {@code tokenType}, {@code expiresIn} y {@code user}.
 */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, sesión y perfil del usuario")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    /**
     * Consulta el perfil persistido del usuario autenticado.
     * @param authorization cabecera con formato Bearer y JWT de acceso
     * @return 200 con el perfil público actual, sin contraseña ni hash
     */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar mi perfil",
            description = "Lee el perfil persistido usando el subject del JWT firmado y vigente del header "
                    + "Authorization: Bearer <token>. No expone la contraseña ni su hash.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = """
                                    {"id":"6710f3a2c9e77c0012345678","name":"Ana Pérez",
                                    "email":"ana.perez@example.com","createdAt":"2026-10-22T10:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"UNAUTHORIZED","message":"Credenciales inválidas",
                                    "details":[],"timestamp":"2026-10-22T10:05:00Z"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"NOT_FOUND","message":"Usuario no encontrado por id: 6710f3a2c9e77c0012345678",
                                    "details":[],"timestamp":"2026-10-22T10:05:00Z"}
                                    """))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"INTERNAL_ERROR","message":"Error interno","details":[],
                                    "timestamp":"2026-10-22T10:05:00Z"}
                                    """)))
    })
    public ResponseEntity<UserResponse> getMe(
            @Parameter(description = "JWT de acceso con prefijo Bearer", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new InvalidCredentialsException();
        }
        return ResponseEntity.ok(userService.getByToken(authorization.substring(BEARER_PREFIX.length())));
    }

    /**
     * Registra un usuario con los datos validados de la solicitud.
     * <p>
     * El email se normaliza antes de guardarse y la contraseña se persiste solo como hash.
     *
     * @param request datos de registro
     * @return 201 con {@code accessToken}, {@code tokenType}, {@code expiresIn} y el perfil público
     *         del usuario creado en {@code user}
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar usuario",
            description = "Crea un usuario con nombre, email y contraseña. El email se normaliza (trim y "
                    + "minúsculas) y debe ser único; la contraseña se guarda solo como hash BCrypt. "
                    + "Devuelve el perfil público del usuario creado y un JWT de acceso.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Datos del usuario a registrar.",
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(name = "Alta válida",
                                    summary = "Registro con datos válidos",
                                    value = """
                                            {"name":"Ana Pérez","email":"ana.perez@example.com",
                                            "password":"password123"}"""))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "Usuario creado", value = """
                                    {
                                      "user": {
                                        "id": "6710f3a2c9e77c0012345678",
                                        "name": "Ana Pérez",
                                        "email": "ana.perez@example.com",
                                        "createdAt": "2026-10-22T10:00:00Z"
                                      },
                                      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                      "tokenType": "Bearer",
                                      "expiresIn": 3600
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o JSON mal formado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Validación", ref = "#/components/examples/ValidationError"),
                                    @ExampleObject(name = "Solicitud mal formada",
                                            ref = "#/components/examples/BadRequest")})),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Email duplicado",
                                    ref = "#/components/examples/DuplicateEmail"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Error interno",
                                    ref = "#/components/examples/InternalError")))
    })
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{id}").buildAndExpand(response.user().id()).toUri()).body(response);
    }

    /**
     * Verifica las credenciales y emite un token de acceso.
     * @param request credenciales validadas
     * @return 200 con {@code accessToken}, {@code tokenType}, {@code expiresIn} y el perfil público
     *         del usuario autenticado en {@code user}
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Iniciar sesión", description = "Normaliza el email y verifica la contraseña con BCrypt. "
            + "Devuelve un JWT y los datos públicos del usuario. Las credenciales incorrectas reciben "
            + "el mismo error tanto si el email no existe como si la contraseña es incorrecta.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Credenciales del usuario ya registrado.",
                    content = @Content(schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(name = "Credenciales válidas", value = """
                                    {
                                      "email": "ana.perez@example.com",
                                      "password": "password123"
                                    }"""))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas y JWT emitido",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":3600,
                                    "user":{"id":"6710f3a2c9e77c0012345678","name":"Ana Pérez",
                                    "email":"ana.perez@example.com","createdAt":"2026-10-22T10:00:00Z"}}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o JSON mal formado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "Validación", ref = "#/components/examples/ValidationError"),
                            @ExampleObject(name = "JSON inválido", ref = "#/components/examples/BadRequest")})),
            @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"UNAUTHORIZED","message":"Credenciales inválidas",
                                    "details":[],"timestamp":"2026-10-22T10:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"code":"INTERNAL_ERROR","message":"Error interno","details":[],
                                    "timestamp":"2026-10-22T10:05:00Z"}
                                    """)))
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * Finaliza la sesión del cliente; el servidor no revoca JWT en esta implementación.
     * @return 204 sin cuerpo, con o sin cabecera Authorization
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "El cliente debe descartar su JWT. "
            + "Responde siempre 204, incluso sin token o con un token inválido; no hay revocación en el servidor.")
    @ApiResponse(responseCode = "204", description = "Sesión finalizada en el cliente", content = @Content)
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}

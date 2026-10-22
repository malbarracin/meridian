package com.meridian.users.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.meridian.users.exception.ErrorResponse;
import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.exception.InvalidCredentialsException;
import com.meridian.users.user.service.UserService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API de registro y consulta de los datos públicos de usuarios.
 * <p>
 * Expone únicamente datos públicos: la contraseña y su hash nunca se devuelven.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro y consulta de los datos públicos de usuarios")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class UserController {

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
                            examples = @ExampleObject(ref = "#/components/examples/NotFound"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InternalError")))
    })
    public ResponseEntity<UserResponse> getMe(
            @Parameter(description = "JWT de acceso con prefijo Bearer", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new InvalidCredentialsException();
        }
        return ResponseEntity.ok(userService.getByToken(authorization.substring(7)));
    }

    /**
     * Registra un usuario con los datos validados de la solicitud.
     * <p>
     * El email se normaliza antes de guardarse y la contraseña se persiste solo como hash.
     *
     * @param request datos de registro
     * @return 201 con los datos públicos del usuario creado y JWT de acceso
     */
    @PostMapping
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
                                      "token": "eyJhbGciOiJIUzI1NiJ9..."
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
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.user().id()).toUri()).body(response);
    }

    /**
     * Consulta los datos públicos de un usuario.
     *
     * @param id identificador del usuario
     * @return 200 con el usuario encontrado
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por id",
            description = "Devuelve los datos públicos de un usuario existente. No expone la contraseña "
                    + "ni su hash.")
    @Parameter(name = "id", description = "Identificador del documento en MongoDB.",
            example = "6710f3a2c9e77c0012345678", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "Usuario encontrado", value = """
                                    {
                                      "id": "6710f3a2c9e77c0012345678",
                                      "name": "Ana Pérez",
                                      "email": "ana.perez@example.com",
                                      "createdAt": "2026-10-22T10:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Usuario inexistente",
                                    ref = "#/components/examples/NotFound"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Error interno",
                                    ref = "#/components/examples/InternalError")))
    })
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }
}

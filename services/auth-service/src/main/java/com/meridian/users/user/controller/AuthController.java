package com.meridian.users.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.users.exception.ErrorResponse;
import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.LoginRequest;
import com.meridian.users.user.service.UserService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** API de autenticación mediante email y contraseña. */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Obtención de tokens de acceso")
public class AuthController {

    private final UserService userService;

    /**
     * Verifica las credenciales y emite un token de acceso.
     * @param request credenciales validadas
     * @return 200 con el perfil público y JWT
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Iniciar sesión", description = "Normaliza el email y verifica la contraseña con BCrypt. "
            + "Devuelve un JWT y los datos públicos del usuario. Las credenciales incorrectas reciben "
            + "el mismo error tanto si el email no existe como si la contraseña es incorrecta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas y JWT emitido",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
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
                            examples = @ExampleObject(ref = "#/components/examples/InternalError")))
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}

package com.meridian.users.user.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.users.exception.ErrorResponse;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.service.UserService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API de consulta de los datos públicos de usuarios.
 * <p>
 * Expone únicamente datos públicos: la contraseña y su hash nunca se devuelven.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Consulta de los datos públicos de usuarios")
public class UserController {

    private final UserService userService;

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

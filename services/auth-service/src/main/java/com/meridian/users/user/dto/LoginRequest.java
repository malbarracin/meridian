package com.meridian.users.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Credenciales de acceso del usuario.
 * @param email email que se normaliza antes de buscar al usuario
 * @param password contraseña en claro que se verifica con BCrypt
 */
@Schema(description = "Credenciales para obtener un JWT de acceso.")
public record LoginRequest(
        @Schema(description = "Email del usuario.", example = "ana@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,
        @Schema(description = "Contraseña del usuario.", example = "password123",
                accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password) {

    /** Recorta el email antes de su validación, igual que en el registro. */
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }

    @Override
    public String toString() {
        return "LoginRequest[datos protegidos]";
    }
}

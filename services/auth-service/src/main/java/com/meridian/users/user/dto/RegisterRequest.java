package com.meridian.users.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos de registro de un usuario.
 * <p>
 * La contraseña se recibe en texto plano únicamente para generar su hash: nunca se persiste ni
 * se devuelve en las respuestas.
 *
 * @param name     nombre del usuario
 * @param email    email del usuario; el servicio lo normaliza (trim y minúsculas) antes de guardarlo
 * @param password contraseña en claro de al menos 8 caracteres
 */
@Schema(description = "Datos necesarios para registrar un usuario.")
public record RegisterRequest(
        @Schema(description = "Nombre del usuario.", example = "Ana Pérez",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String name,

        @Schema(description = "Email del usuario. Se acepta cualquier variante de mayúsculas y espacios: "
                + "el servicio guarda la versión normalizada.", example = "ana.perez@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "Contraseña en texto plano de al menos 8 caracteres. Solo se usa para generar "
                + "el hash BCrypt que se persiste.", example = "password123", minLength = 8,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 8) String password) {

    /** Recorta el email antes de que Bean Validation evalúe el registro. */
    public RegisterRequest {
        if (email != null) {
            email = email.trim();
        }
    }

    @Override
    public String toString() {
        return "RegisterRequest[datos protegidos]";
    }
}

package com.meridian.users.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfil público y token de acceso emitidos tras un registro exitoso.
 *
 * @param user datos públicos del usuario persistido, sin contraseña ni hash
 * @param token JWT firmado para usar como Authorization Bearer
 */
@Schema(description = "Respuesta de registro exitoso: perfil público del usuario y JWT de acceso.")
public record AuthResponse(
        @Schema(description = "Datos públicos del usuario recién creado.")
        UserResponse user,

        @Schema(description = "JWT firmado (HS256) listo para usar en cabeceras Authorization Bearer.",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String token) {
}

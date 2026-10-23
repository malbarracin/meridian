package com.meridian.users.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfil público y sesión emitidos tras un registro o login exitoso.
 *
 * @param user datos públicos del usuario persistido, sin contraseña ni hash
 * @param accessToken JWT firmado para usar como Authorization Bearer
 * @param tokenType esquema de autorización Bearer
 * @param expiresIn vigencia configurada del JWT en segundos
 */
@Schema(description = "Respuesta de registro o login exitoso: perfil público del usuario y JWT de acceso.")
public record AuthResponse(
        @Schema(description = "Datos públicos del usuario autenticado.")
        UserResponse user,

        @Schema(description = "JWT firmado (HS256) listo para usar en cabeceras Authorization Bearer.",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Esquema de autorización.", example = "Bearer", allowableValues = {"Bearer"})
        String tokenType,

        @Schema(description = "Vigencia del token en segundos.", example = "3600")
        long expiresIn) {

    public static final String TOKEN_TYPE = "Bearer";

}

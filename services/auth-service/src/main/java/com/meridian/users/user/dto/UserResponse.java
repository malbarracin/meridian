package com.meridian.users.user.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos públicos de un usuario registrado.
 * <p>
 * No incluye la contraseña ni su hash.
 *
 * @param id        identificador del documento en MongoDB
 * @param name      nombre del usuario
 * @param email     email normalizado (trim y minúsculas)
 * @param createdAt fecha de alta en ISO-8601 UTC
 */
@Schema(description = "Datos públicos del usuario, sin contraseña ni hash.")
public record UserResponse(
        @Schema(description = "Identificador del documento en MongoDB.", example = "6710f3a2c9e77c0012345678",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String id,

        @Schema(description = "Nombre del usuario.", example = "Ana Pérez",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Email normalizado con trim y minúsculas.", example = "ana.perez@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Fecha de alta en ISO-8601 UTC.", example = "2026-10-22T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt) {
}

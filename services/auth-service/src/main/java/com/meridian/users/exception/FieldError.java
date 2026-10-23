package com.meridian.users.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error de validación de un campo de la solicitud.
 *
 * @param field   nombre del campo rechazado
 * @param message motivo del rechazo
 */
@Schema(description = "Campo rechazado por la validación de la solicitud.")
public record FieldError(
        @Schema(description = "Nombre del campo rechazado.", example = "email",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String field,

        @Schema(description = "Motivo del rechazo.", example = "must be a well-formed email address",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {
}

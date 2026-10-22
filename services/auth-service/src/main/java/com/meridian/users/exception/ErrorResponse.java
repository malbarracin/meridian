package com.meridian.users.exception;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contrato de error compartido por la API.
 *
 * @param code      código estable del contrato de errores
 * @param message   mensaje legible para una persona, sin detalles internos
 * @param details   errores por campo; lista vacía cuando no hay nada que detallar
 * @param timestamp momento del error en ISO-8601 UTC
 */
@Schema(description = "Respuesta de error uniforme de la API.")
public record ErrorResponse(
        @Schema(description = "Código estable del contrato de errores.",
                example = "VALIDATION_ERROR",
                allowableValues = {"VALIDATION_ERROR", "BAD_REQUEST", "NOT_FOUND", "CONFLICT", "INTERNAL_ERROR"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Mensaje legible, sin stacktrace ni datos internos.",
                example = "La solicitud tiene datos inválidos", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,

        @Schema(description = "Detalle por campo; arreglo vacío si no aplica.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FieldError> details,

        @Schema(description = "Momento del error en ISO-8601 UTC.", example = "2026-10-22T10:05:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant timestamp) {

    public ErrorResponse {
        details = List.copyOf(details);
    }
}

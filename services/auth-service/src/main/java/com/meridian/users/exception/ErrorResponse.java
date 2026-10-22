package com.meridian.users.exception;

import java.time.Instant;
import java.util.List;

/** Contrato de error compartido por la API. */
public record ErrorResponse(ErrorCode code, String message, List<FieldError> details, Instant timestamp) {

    public ErrorResponse {
        details = List.copyOf(details);
    }
}

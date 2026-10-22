package com.meridian.dashboard.exception;

import java.time.Instant;
import java.util.List;

/** Error público con detalles inmutables y fecha UTC. */
public record ErrorResponse(ErrorCode code, String message, List<FieldError> details, Instant timestamp) {

    public ErrorResponse {
        details = List.copyOf(details);
    }
}

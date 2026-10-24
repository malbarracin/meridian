package com.meridian.dashboard.exception;

import java.time.Instant;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/** Traduce excepciones al contrato público sin exponer detalles internos. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Devuelve los parámetros rechazados por las restricciones del endpoint. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException exception) {
        List<FieldError> details = exception.getParameterValidationResults().stream()
                .map(result -> new FieldError(result.getMethodParameter().getParameterName(), "valor no soportado"))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "La solicitud tiene datos inválidos", details);
    }

    /** Rechaza credenciales sin exponer el motivo criptográfico. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException exception) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, exception.getMessage(), List.of());
    }

    /** Identifica parámetros fuera del catálogo soportado. */
    @ExceptionHandler(UnsupportedParameterException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedParameter(UnsupportedParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, exception.getMessage(),
                List.of(new FieldError(exception.getField(), "valor no soportado")));
    }

    /** Devuelve los campos rechazados por la validación del cuerpo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "La solicitud tiene datos inválidos", details);
    }

    /** Devuelve las restricciones incumplidas en parámetros. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldError> details = exception.getConstraintViolations().stream()
                .map(error -> new FieldError(error.getPropertyPath().toString(), error.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "La solicitud tiene datos inválidos", details);
    }

    /** Rechaza solicitudes mal formadas sin revelar el contenido recibido. */
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Solicitud inválida", List.of());
    }

    /** Informa de la ausencia de un recurso. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingRoute(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Recurso no encontrado", List.of());
    }

    /** Informa de la ausencia de un recurso de dominio. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, exception.getMessage(), List.of());
    }

    /** Informa de un conflicto de dominio. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, exception.getMessage(), List.of());
    }

    /** Devuelve un mensaje genérico ante errores inesperados. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Error inesperado de tipo {}", exception.getClass().getName());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Error interno", List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message,
            List<FieldError> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, details, Instant.now()));
    }
}

package com.meridian.users.exception;

/** Error de validación de un campo de la solicitud. */
public record FieldError(String field, String message) {
}

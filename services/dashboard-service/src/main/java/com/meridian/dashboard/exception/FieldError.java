package com.meridian.dashboard.exception;

/** Restricción incumplida en un campo de la solicitud. */
public record FieldError(String field, String message) {
}

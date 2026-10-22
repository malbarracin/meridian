package com.meridian.dashboard.exception;

/** Base de los errores de recurso inexistente. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}

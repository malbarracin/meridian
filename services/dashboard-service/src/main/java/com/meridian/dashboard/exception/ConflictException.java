package com.meridian.dashboard.exception;

/** Base de los conflictos de dominio. */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}

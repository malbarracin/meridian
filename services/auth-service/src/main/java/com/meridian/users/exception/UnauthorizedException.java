package com.meridian.users.exception;

/** Base de los errores de autenticación del servicio. */
public abstract class UnauthorizedException extends RuntimeException {

    protected UnauthorizedException(String message) {
        super(message);
    }
}

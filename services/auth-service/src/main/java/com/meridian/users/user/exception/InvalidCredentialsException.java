package com.meridian.users.user.exception;

import com.meridian.users.exception.UnauthorizedException;

/** Credenciales rechazadas sin revelar si el email existe. */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}

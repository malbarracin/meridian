package com.meridian.dashboard.exception;

/** Credenciales ausentes o rechazadas, sin revelar información del token. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Credenciales inválidas");
    }
}

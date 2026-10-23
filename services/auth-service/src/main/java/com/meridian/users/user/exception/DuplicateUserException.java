package com.meridian.users.user.exception;

import com.meridian.users.exception.ConflictException;

/** Conflicto al registrar un email existente. */
public class DuplicateUserException extends ConflictException {

    public DuplicateUserException() {
        super("Email ya registrado");
    }
}

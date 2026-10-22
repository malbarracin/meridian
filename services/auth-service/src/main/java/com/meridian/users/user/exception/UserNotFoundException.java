package com.meridian.users.user.exception;

import com.meridian.users.exception.NotFoundException;

/** Ausencia de un usuario solicitado por identificador. */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String id) {
        super("Usuario no encontrado por id: " + id);
    }
}

package com.meridian.users.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de registro; la contraseña se recibe en texto plano únicamente para generar su hash. */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[datos protegidos]";
    }
}

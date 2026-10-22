package com.meridian.users.user.dto;

import java.time.Instant;

/** Datos públicos del usuario, sin contraseña ni hash. */
public record UserResponse(String id, String name, String email, Instant createdAt) {
}

package com.meridian.dashboard.security;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.jsonwebtoken.security.WeakKeyException;

/** Clave compartida con auth-service, obligatoria y de al menos 32 bytes UTF-8. */
@ConfigurationProperties("jwt")
public record JwtConfig(String secret) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public JwtConfig {
        Objects.requireNonNull(secret, "JWT_SECRET es obligatorio");
        if (secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new WeakKeyException("JWT_SECRET debe contener al menos 32 bytes y no puede estar en blanco");
        }
    }

    @Override
    public String toString() {
        return "JwtConfig[secret=REDACTED]";
    }
}

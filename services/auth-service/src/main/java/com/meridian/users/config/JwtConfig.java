package com.meridian.users.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.jsonwebtoken.security.Keys;

/**
 * Configuracion de firma y vigencia de los tokens JWT.
 *
 * @param secret clave HMAC-SHA256 proporcionada por el entorno (minimo 32 bytes)
 * @param expirationMs vigencia del token en milisegundos
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(String secret, long expirationMs) {

    /** Valida la clave de firma al crear la configuracion, antes de aceptar solicitudes. */
    public JwtConfig {
        Objects.requireNonNull(secret, "JWT_SECRET es obligatorio");
        Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

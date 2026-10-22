package com.meridian.users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de firma y vigencia de los tokens JWT.
 *
 * @param secret clave HMAC-SHA256 proporcionada por el entorno (minimo 32 bytes)
 * @param expirationMs vigencia del token en milisegundos
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(String secret, long expirationMs) {
}

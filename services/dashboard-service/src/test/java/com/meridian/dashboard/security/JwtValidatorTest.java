package com.meridian.dashboard.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validación criptográfica JWT")
public class JwtValidatorTest {

    public static final String SECRET = "dashboard-test-secret-with-at-least-32-bytes";

    public static String token(String secret, Instant expiration) {
        var builder = Jwts.builder();
        if (expiration != null) {
            builder.expiration(Date.from(expiration));
        }
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    @DisplayName("Acepta HS256 vigente y rechaza firma distinta, expiración, ausencia de exp y formatos inválidos")
    void validatesSignatureAndExpiration() {
        var validator = new JwtValidator(new JwtConfig(SECRET));
        assertThat(validator.isValid(token(SECRET, Instant.now().plusSeconds(300)))).isTrue();
        assertThat(validator.isValid(token(SECRET, Instant.now().minusSeconds(300)))).isFalse();
        assertThat(validator.isValid(token("other-secret-with-at-least-thirty-two-bytes",
                Instant.now().plusSeconds(300)))).isFalse();
        assertThat(validator.isValid(token(SECRET, null))).isFalse();
        assertThat(validator.isValid("malformed")).isFalse();
        assertThat(validator.isValid("")).isFalse();
        assertThat(validator.isValid(null)).isFalse();
    }
}

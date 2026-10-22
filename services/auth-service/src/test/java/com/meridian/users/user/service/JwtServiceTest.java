package com.meridian.users.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.meridian.users.config.JwtConfig;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-32chars";
    private static final long EXPIRATION_MS = 3_600_000;
    private static final String USER_ID = "user-1";
    private static final String EMAIL = "ada@example.com";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(new JwtConfig(SECRET, EXPIRATION_MS));

    @Test
    @DisplayName("Genera un JWT HS256 valido con identidad, email normalizado y vigencia configurada")
    void generaTokenConClaimsYVigenciaConfigurados() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generateToken(USER_ID, " ADA@EXAMPLE.COM ");

        Instant after = Instant.now();
        assertThat(token).isNotBlank();
        Jws<Claims> parsedToken = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token);
        assertThat(jwtService.isValid(token)).isTrue();
        Claims claims = parsedToken.getPayload();
        assertThat(parsedToken.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo(USER_ID);
        assertThat(claims.get("email", String.class)).isEqualTo(EMAIL);
        assertThat(claims.getIssuedAt().toInstant()).isBetween(before, after);
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    @DisplayName("Rechaza un token cuya firma fue alterada")
    void rechazaTokenAlterado() {
        String token = jwtService.generateToken(USER_ID, EMAIL);
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String alteredToken = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        assertThat(jwtService.isValid(alteredToken)).isFalse();
    }

    @Test
    @DisplayName("Rechaza un token firmado que ya expiro sin esperar en el test")
    void rechazaTokenExpirado() {
        String token = Jwts.builder()
                .subject(USER_ID)
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid.token", "not-a-jwt"})
    @DisplayName("Rechaza tokens nulos, vacios o mal formados")
    void rechazaTokenMalFormado(String token) {
        assertThat(jwtService.isValid(token)).isFalse();
    }
}

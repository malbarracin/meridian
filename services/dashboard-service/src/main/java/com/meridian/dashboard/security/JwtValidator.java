package com.meridian.dashboard.security;

import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Verifica tokens de acceso sin consultar usuarios ni emitir tokens. */
public class JwtValidator {

    private final JwtParser parser;

    public JwtValidator(JwtConfig config) {
        parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    /** @return true solo para tokens HS256 firmados correctamente y con expiración vigente */
    public boolean isValid(String token) {
        try {
            var claims = parser.parseSignedClaims(token);
            return Jwts.SIG.HS256.getId().equals(claims.getHeader().getAlgorithm())
                    && claims.getPayload().getExpiration() != null;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}

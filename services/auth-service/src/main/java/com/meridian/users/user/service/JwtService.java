package com.meridian.users.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.meridian.users.config.JwtConfig;
import com.meridian.users.user.exception.InvalidCredentialsException;

/** Genera y valida tokens de acceso firmados con HMAC-SHA256. */
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    /**
     * Valida y prepara la clave antes de permitir cualquier operacion con tokens.
     *
     * @param jwtConfig configuracion con una clave de al menos 32 bytes para HS256
     */
    public JwtService(JwtConfig jwtConfig) {
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtConfig = jwtConfig;
    }

    /**
     * Genera un token con el identificador y el email normalizado del usuario.
     *
     * @param userId identificador del usuario para el subject
     * @param email email del usuario
     * @return JWT firmado con la vigencia configurada
     */
    public String generateToken(String userId, String email) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email.trim().toLowerCase(Locale.ROOT))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(jwtConfig.expirationMs())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica la firma HS256 y la expiracion del token.
     *
     * @param token JWT recibido, que puede ser nulo o mal formado
     * @return true si la firma es valida y el token no ha expirado
     */
    public boolean isValid(String token) {
        try {
            return Jwts.SIG.HS256.getId().equals(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getHeader().getAlgorithm());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Extrae la identidad tras verificar firma HS256, expiración y subject, sin acceder a la base.
     * @param token JWT de acceso
     * @return identificador autenticado
     * @throws InvalidCredentialsException si el token no es un token de acceso válido
     */
    public String extractUserId(String token) {
        try {
            var signedClaims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            String userId = signedClaims.getPayload().getSubject();
            if (!Jwts.SIG.HS256.getId().equals(signedClaims.getHeader().getAlgorithm())
                    || signedClaims.getPayload().getExpiration() == null || userId == null || userId.isBlank()) {
                throw new InvalidCredentialsException();
            }
            return userId;
        } catch (JwtException | IllegalArgumentException exception) {
            InvalidCredentialsException unauthorized = new InvalidCredentialsException();
            unauthorized.initCause(exception);
            throw unauthorized;
        }
    }

}

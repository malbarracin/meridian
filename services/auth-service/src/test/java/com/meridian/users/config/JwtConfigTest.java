package com.meridian.users.config;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final String TEST_SECRET = "test-secret-key-for-unit-tests-only-32chars";
    private static final long TEST_EXPIRATION_MS = 3_600_000L;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtPropertiesConfiguration.class);

    @Test
    @DisplayName("Carga jwt.secret y jwt.expiration-ms desde propiedades")
    void cargaPropiedadesJwt() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=" + TEST_SECRET,
                        "jwt.expiration-ms=" + TEST_EXPIRATION_MS)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JwtConfig jwtConfig = context.getBean(JwtConfig.class);
                    assertThat(jwtConfig.secret()).isEqualTo(TEST_SECRET);
                    assertThat(jwtConfig.secret()).hasSizeGreaterThanOrEqualTo(32);
                    assertThat(jwtConfig.expirationMs()).isEqualTo(TEST_EXPIRATION_MS);
                });
    }

    @Test
    @DisplayName("Falla el arranque si jwt.expiration-ms no es numérico")
    void fallaConExpirationInvalida() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=" + TEST_SECRET,
                        "jwt.expiration-ms=no-es-numero")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("El secreto de test es válido para HS256 con JJWT")
    void secretDeTestEsValidoParaHs256() {
        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);

        assertThatCode(() -> Keys.hmacShaKeyFor(keyBytes)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JJWT rechaza una clave HMAC demasiado corta")
    void jjwtRechazaClaveCorta() {
        byte[] keyBytes = "too-short".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> Keys.hmacShaKeyFor(keyBytes))
                .isInstanceOf(WeakKeyException.class);
    }

    @Configuration
    @EnableConfigurationProperties(JwtConfig.class)
    static class JwtPropertiesConfiguration {
    }
}

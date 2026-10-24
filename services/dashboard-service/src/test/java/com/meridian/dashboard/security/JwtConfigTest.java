package com.meridian.dashboard.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Configuración JWT obligatoria al arrancar")
class JwtConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("No arranca sin secreto, con secreto corto ni en blanco")
    void failsWithoutValidSecret() {
        runner.run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("jwt.secret=short").run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("jwt.secret=                                ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Arranca con secreto válido y no lo expone en toString")
    void startsWithValidSecret() {
        runner.withPropertyValues("jwt.secret=" + JwtValidatorTest.SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JwtConfig.class).toString()).doesNotContain(JwtValidatorTest.SECRET);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtConfig.class)
    static class PropertiesConfig {
    }
}

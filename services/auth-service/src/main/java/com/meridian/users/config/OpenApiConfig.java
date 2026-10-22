package com.meridian.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Identidad de la documentación pública del servicio. */
@Configuration
public class OpenApiConfig {

    /** @return metadatos de la API de autenticación */
    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI().info(new Info().title("Auth Service API").version("1.0"));
    }
}

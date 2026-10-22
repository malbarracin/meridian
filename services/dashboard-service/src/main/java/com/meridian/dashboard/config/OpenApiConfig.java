package com.meridian.dashboard.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Agrupa el contrato público del dashboard. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder().group("dashboard").pathsToMatch("/api/v1/dashboard/**").build();
    }
}

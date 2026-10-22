package com.meridian.dashboard.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Agrupación OpenAPI del dashboard")
class OpenApiConfigTest {

    @Test
    @DisplayName("Expone el grupo dashboard bajo /api/v1/dashboard")
    void groupsDashboardPaths() {
        GroupedOpenApi groupedOpenApi = new OpenApiConfig().dashboardApi();

        assertThat(groupedOpenApi.getGroup()).isEqualTo("dashboard");
        assertThat(groupedOpenApi.getPathsToMatch()).containsExactly("/api/v1/dashboard/**");
    }
}

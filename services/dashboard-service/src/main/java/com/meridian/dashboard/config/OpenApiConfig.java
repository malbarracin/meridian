package com.meridian.dashboard.config;

import java.time.Instant;
import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meridian.dashboard.dashboard.dto.SummaryResponse;
import com.meridian.dashboard.dashboard.dto.NewsResponse;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/** Metadatos, ejemplos y agrupación del contrato público del dashboard. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Define la identidad del servicio, el servidor local y los ejemplos reutilizables.
     *
     * @return especificación base de la API del dashboard
     */
    @Bean
    public OpenAPI dashboardServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dashboard Service API")
                        .version("1.0")
                        .description("Datos públicos agregados de dólar, FX, criptomonedas, bolsa y noticias "
                                + "de IA/tecnología. Los fallos de un proveedor se informan en la lista "
                                + "degraded sin cambiar el status HTTP. Con DASHBOARD_USE_MOCK=true las "
                                + "respuestas usan fixtures estáticas y el ejemplo de este contrato."))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Entorno local")))
                .components(sharedComponents());
    }

    @Bean
    GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("dashboard")
                .displayName("Dashboard")
                .pathsToMatch("/api/v1/dashboard/**")
                .build();
    }

    private Components sharedComponents() {
        return new Components()
                .addExamples("NewsMock", new Example()
                        .summary("Noticias en modo mock")
                        .value(OBJECT_MAPPER.valueToTree(new NewsResponse(DashboardMockFixtures.getNews(), false))))
                .addExamples("SummaryMock", new Example()
                        .summary("Dashboard en modo mock")
                        .description("Respuesta completa con DASHBOARD_USE_MOCK=true; los valores son "
                                + "fixtures estáticas y no cotizaciones reales.")
                        .value(mockDashboardExample()))
                .addExamples("InternalError", new Example()
                        .summary("Error interno")
                        .description("Error inesperado durante la orquestación; no expone detalles internos.")
                        .value(internalErrorExample()));
    }

    private JsonNode mockDashboardExample() {
        SummaryResponse response = new SummaryResponse("Meridian", Instant.parse("2026-10-22T10:05:00Z"),
                DashboardMockFixtures.getSummaryFx(), DashboardMockFixtures.getSummaryCrypto(),
                DashboardMockFixtures.getSummaryEquities(), List.of());
        return OBJECT_MAPPER.valueToTree(response);
    }

    private ObjectNode internalErrorExample() {
        ObjectNode example = OBJECT_MAPPER.createObjectNode();
        example.put("code", "INTERNAL_ERROR");
        example.put("message", "Error interno");
        example.set("details", OBJECT_MAPPER.createArrayNode());
        example.put("timestamp", "2026-10-22T10:05:00Z");
        return example;
    }
}

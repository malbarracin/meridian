package com.meridian.dashboard.docs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exporta la especificación OpenAPI generada por springdoc a
 * {@code delivery/openapi/dashboard-service/} para que la copia versionada coincida con el código.
 *
 * <p>El servicio comparte la carpeta {@code delivery/openapi} con el resto de los servicios del
 * repositorio, por eso este módulo escribe en su propio subdirectorio y no reemplaza el contrato
 * de otros servicios.</p>
 */
@SpringBootTest(properties = "DASHBOARD_USE_MOCK=true")
@AutoConfigureMockMvc
class OpenApiExportTest {

    private static final Path OUTPUT_DIRECTORY =
            Path.of("..", "..", "delivery", "openapi", "dashboard-service").normalize();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("El build exporta openapi.json y openapi.yaml con el endpoint y sus respuestas")
    void exportaEspecificacionOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String yaml = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Files.createDirectories(OUTPUT_DIRECTORY);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode specification = mapper.readTree(json);
        Files.writeString(OUTPUT_DIRECTORY.resolve("openapi.json"), specification.toPrettyString(),
                StandardCharsets.UTF_8);
        Files.writeString(OUTPUT_DIRECTORY.resolve("openapi.yaml"), yaml, StandardCharsets.UTF_8);

        JsonNode paths = specification.path("paths");
        assertThat(paths.has("/api/v1/dashboard/summary")).isTrue();
        JsonNode operation = paths.path("/api/v1/dashboard/summary").path("get");
        assertThat(operation.path("summary").asText()).isNotBlank();
        assertThat(paths.has("/api/v1/dashboard")).isFalse();
        assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(operation.path("responses").has("401")).isTrue();
        assertThat(operation.path("responses").has("200")).isTrue();
        assertThat(operation.path("responses").has("500")).isTrue();
        assertThat(operation.path("responses").path("200").path("content")
                .path("application/json").path("examples").has("modoMock")).isTrue();
        assertThat(operation.path("responses").path("500").path("content")
                .path("application/json").path("examples").has("errorInterno")).isTrue();
        assertThat(specification.path("components").path("examples").has("SummaryMock")).isTrue();
        assertThat(specification.path("components").path("schemas").has("SummaryResponse")).isTrue();
    }
}

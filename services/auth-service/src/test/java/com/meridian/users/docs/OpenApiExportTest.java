package com.meridian.users.docs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.users.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exporta la especificación OpenAPI generada por springdoc a {@code delivery/openapi/} para que la
 * copia versionada coincida siempre con el código.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
class OpenApiExportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("El build exporta openapi.json y openapi.yaml con todos los endpoints")
    void exportaEspecificacionOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String yaml = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Path outputDirectory = Path.of("..", "..", "delivery", "openapi").normalize();
        Files.createDirectories(outputDirectory);
        ObjectMapper mapper = new ObjectMapper();
        Files.writeString(outputDirectory.resolve("openapi.json"), mapper.readTree(json).toPrettyString(),
                StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve("openapi.yaml"), yaml, StandardCharsets.UTF_8);

        JsonNode paths = mapper.readTree(json).path("paths");
        assertThat(paths.has("/api/v1/auth/register")).isTrue();
        assertThat(paths.has("/api/v1/users/{id}")).isTrue();
        assertThat(paths.has("/api/v1/auth/login")).isTrue();
        assertThat(paths.has("/api/v1/auth/me")).isTrue();
        assertThat(paths.has("/api/v1/auth/logout")).isTrue();
        assertThat(paths.has("/api/v1/users")).isFalse();
        assertThat(paths.path("/api/v1/auth/register").path("post").has("requestBody")).isTrue();
        assertThat(paths.path("/api/v1/auth/register").path("post").path("responses").has("409")).isTrue();
        assertThat(paths.path("/api/v1/users/{id}").path("get").path("responses").has("404")).isTrue();
    }
}

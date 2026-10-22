package com.meridian.users.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meridian.users.user.service.UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("OpenAPI publica metadatos, endpoints y esquemas de respuesta")
    void publicaOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Auth Service API"))
                .andExpect(jsonPath("$.info.version").value("1.0"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.UserResponse.properties.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("OpenAPI documenta el registro con perfil público y token de acceso")
    void documentaRegistroConAuthResponse() throws Exception {
        String responsesPath = "$.paths['/api/v1/users'].post.responses";
        String createdContentPath = responsesPath + "['201'].content['application/json']";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(createdContentPath + ".schema['$ref']")
                        .value("#/components/schemas/AuthResponse"))
                .andExpect(jsonPath(createdContentPath + ".examples['Usuario creado'].value.user.id").isNotEmpty())
                .andExpect(jsonPath(createdContentPath + ".examples['Usuario creado'].value.user.email").isNotEmpty())
                .andExpect(jsonPath(createdContentPath + ".examples['Usuario creado'].value.token").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.user['$ref']")
                        .value("#/components/schemas/UserResponse"))
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.token.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.passwordHash").doesNotExist())
                .andExpect(jsonPath(responsesPath + "['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath(responsesPath + "['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").doesNotExist());
    }

    @Test
    @DisplayName("Swagger UI está disponible desde la ruta configurada")
    void publicaSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}

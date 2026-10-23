package com.meridian.users.user.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.users.MongoTestContainer;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H2.3 — GET /api/v1/auth/me contra MongoDB real (Testcontainers): perfil persistido y rechazos 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProfileIntegrationTest extends MongoTestContainer {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String PROFILE_PATH = "/api/v1/auth/me";
    private static final String NAME = "Usuario Ejemplo";
    private static final String EMAIL = "usuario@ejemplo.com";
    private static final String REGISTER_BODY = """
            {"name":"Usuario Ejemplo","email":"usuario@ejemplo.com","password":"miPassword123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limpiaUsuarios() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /me con JWT de registro responde 200 con el perfil persistido sin passwordHash")
    void meLeePerfilPersistido() throws Exception {
        MvcResult registerResult = mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String userId = registerBody.path("user").path("id").asText();
        String token = registerBody.path("accessToken").asText();
        User persisted = userRepository.findById(userId).orElseThrow();

        mockMvc.perform(get(PROFILE_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(persisted.getName()).isEqualTo(NAME);
        assertThat(persisted.getEmail()).isEqualTo(EMAIL);
        assertThat(persisted.getPasswordHash()).isNotBlank().doesNotContain("miPassword123");
    }

    @Test
    @DisplayName("GET /me con JWT alterado responde 401 UNAUTHORIZED con contrato de error")
    void meConTokenInvalido401() throws Exception {
        MvcResult registerResult = mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("accessToken").asText();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String alteredToken = token.substring(0, signatureStart) + replacement
                + token.substring(signatureStart + 1);

        mockMvc.perform(get(PROFILE_PATH).header("Authorization", "Bearer " + alteredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("GET /me sin Authorization responde 401 UNAUTHORIZED con contrato de error")
    void meConTokenAusente401() throws Exception {
        mockMvc.perform(get(PROFILE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}

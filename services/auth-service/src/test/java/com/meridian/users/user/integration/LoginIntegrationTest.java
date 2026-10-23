package com.meridian.users.user.integration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.users.MongoTestContainer;
import com.meridian.users.config.JwtConfig;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H2.3 — login contra MongoDB real (Testcontainers): éxito tras registro y rechazos 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginIntegrationTest extends MongoTestContainer {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String EMAIL = "usuario@ejemplo.com";
    private static final String REGISTER_BODY = """
            {"name":"Usuario Ejemplo","email":"usuario@ejemplo.com","password":"miPassword123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limpiaUsuarios() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Login correcto tras registro responde 200 con JWT cuyo subject es el id del usuario")
    void loginCorrecto200ConJwt() throws Exception {
        MvcResult registerResult = mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("user").path("id").asText();

        MvcResult loginResult = mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@ejemplo.com","password":"miPassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value((int) (jwtConfig.expirationMs() / 1000)))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        assertThat(body.get("expiresIn").asLong()).isPositive();
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtConfig.secret().getBytes(StandardCharsets.UTF_8));
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        assertThat(subject).isEqualTo(userId);
        assertThat(userRepository.findByEmail(EMAIL)).isPresent();
    }

    @Test
    @DisplayName("Login con password incorrecto responde 401 UNAUTHORIZED con contrato de error")
    void loginPasswordIncorrecto401() throws Exception {
        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@ejemplo.com","password":"otraPassword999"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Login con email inexistente responde 401 UNAUTHORIZED con el mismo mensaje genérico")
    void loginEmailInexistente401() throws Exception {
        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"noexiste@ejemplo.com","password":"miPassword123"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}

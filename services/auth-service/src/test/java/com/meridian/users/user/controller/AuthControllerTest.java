package com.meridian.users.user.controller;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meridian.users.config.JwtConfig;
import com.meridian.users.config.SecurityConfig;
import com.meridian.users.user.dto.LoginRequest;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.mapper.UserMapper;
import com.meridian.users.user.repository.UserRepository;
import com.meridian.users.user.service.JwtService;
import com.meridian.users.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({UserService.class, SecurityConfig.class, AuthControllerTest.LoginConfiguration.class})
class AuthControllerTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = "password123";
    private static final String BODY = """
            {"email":"  ANA@EXAMPLE.COM  ","password":"password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void preparaUsuario() {
        user = new User();
        user.setId("user-1");
        user.setName("Ana");
        user.setEmail(EMAIL);
        user.setCreatedAt(Instant.parse("2026-10-22T10:00:00Z"));
        user.setPasswordHash(new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @Test
    @DisplayName("El login normaliza email, verifica BCrypt y devuelve perfil público y JWT firmado")
    void autenticaConCredencialesValidas() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        String response = mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.user", aMapWithSize(4)))
                .andExpect(jsonPath("$.user.id").value("user-1"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("token").asText();
        assertThat(jwtService.isValid(token)).isTrue();
        verify(userRepository).findByEmail(EMAIL);
        verify(userRepository, never()).save(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Email inexistente y contraseña incorrecta devuelven el mismo error 401 sin token")
    void rechazaCredencialesInvalidas(boolean userExists) throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(userExists ? Optional.of(user) : Optional.empty());

        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"incorrecta"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"email\":\"invalido\",\"password\":\"password123\"}",
            "{\"email\":\"ana@example.com\",\"password\":\" \"}",
            "{\"email\":\" \",\"password\":\"password123\"}"
    })
    @DisplayName("Los campos ausentes o inválidos devuelven 400 antes de consultar usuarios")
    void validaCredencialesAntesDeAutenticar(String body) throws Exception {
        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isNotEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("El DTO protege las credenciales en su representación textual")
    void ocultaCredencialesEnTexto() {
        assertThat(new LoginRequest(EMAIL, PASSWORD).toString()).doesNotContain(EMAIL, PASSWORD);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LoginConfiguration {

        @Bean
        UserMapper userMapper() {
            return Mappers.getMapper(UserMapper.class);
        }

        @Bean
        JwtService jwtService() {
            return new JwtService(new JwtConfig("test-secret-at-least-32-bytes-long-login", 60000));
        }
    }
}

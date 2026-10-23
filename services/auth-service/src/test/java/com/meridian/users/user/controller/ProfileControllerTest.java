package com.meridian.users.user.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meridian.users.config.JwtConfig;
import com.meridian.users.exception.GlobalExceptionHandler;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.mapper.UserMapper;
import com.meridian.users.user.repository.UserRepository;
import com.meridian.users.user.service.JwtService;
import com.meridian.users.user.service.UserService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest {

    private static final String SECRET = "profile-tests-signing-secret-at-least-32-bytes";
    private static final String USER_ID = "507f1f77bcf86cd799439011";
    private static final String PROFILE_PATH = "/api/v1/auth/me";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final UserRepository repository = mock(UserRepository.class);
    private final JwtService jwtService = new JwtService(new JwtConfig(SECRET, 3_600_000));
    private MockMvc mockMvc;

    @BeforeEach
    void configuraEndpointConServiciosReales() {
        UserService userService = new UserService(repository, mock(PasswordEncoder.class),
                Mappers.getMapper(UserMapper.class), jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        JsonMapper.builder().findAndAddModules()
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    @DisplayName("Me consulta el perfil actual por el subject y nunca expone contraseña ni hash")
    void consultaPerfilActualPorSubject() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Nombre actualizado");
        user.setEmail("actual@example.com");
        user.setPasswordHash("hash-secreto");
        user.setCreatedAt(Instant.parse("2026-10-22T10:00:00Z"));
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
        String token = jwtService.generateToken(USER_ID, "anterior@example.com");

        mockMvc.perform(get(PROFILE_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository).findById(USER_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer ", "Bearer not-a-jwt"})
    @DisplayName("Me rechaza headers ausentes o inválidos con 401 antes de consultar la base")
    void rechazaHeaderInvalido(String authorization) throws Exception {
        MockHttpServletRequestBuilder request = get(PROFILE_PATH);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "wrong-key", "missing-subject", "missing-expiration", "blank-subject"})
    @DisplayName("Me rechaza JWT expirado, firma ajena o claims obligatorios ausentes con 401")
    void rechazaJwtInvalido(String invalidCase) throws Exception {
        String subject = switch (invalidCase) {
            case "missing-subject" -> null;
            case "blank-subject" -> " ";
            default -> USER_ID;
        };
        Date expiration = "missing-expiration".equals(invalidCase) ? null
                : Date.from(Instant.now().plusSeconds("expired".equals(invalidCase) ? -60 : 3600));
        SecretKey key = "wrong-key".equals(invalidCase) ? Jwts.SIG.HS256.key().build() : KEY;
        String token = Jwts.builder().subject(subject).expiration(expiration)
                .signWith(key, Jwts.SIG.HS256).compact();

        mockMvc.perform(get(PROFILE_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Me devuelve 404 si el usuario de un JWT válido ya no existe")
    void devuelve404ParaUsuarioEliminado() throws Exception {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(PROFILE_PATH).header("Authorization",
                        "Bearer " + jwtService.generateToken(USER_ID, "ana@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}

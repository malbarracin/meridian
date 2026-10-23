package com.meridian.users.user.controller;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.exception.DuplicateUserException;
import com.meridian.users.user.exception.UserNotFoundException;
import com.meridian.users.user.service.UserService;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_ID = "507f1f77bcf86cd799439011";
    private static final RegisterRequest REQUEST = new RegisterRequest("Ana", "ana@example.com", "password123");
    private static final UserResponse RESPONSE = new UserResponse(USER_ID, "Ana", "ana@example.com",
            Instant.parse("2026-10-22T10:00:00Z"));
    private static final String VALID_BODY = """
            {"name":"Ana","email":"ana@example.com","password":"password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/users devuelve 201 con AuthResponse (user + token) sin passwordHash")
    void registraUsuarioSinExponerContrasena() throws Exception {
        when(userService.register(REQUEST)).thenReturn(new AuthResponse(RESPONSE, "signed-token"));

        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + USERS_PATH + "/" + USER_ID))
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.token").value("signed-token"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user", aMapWithSize(4)))
                .andExpect(jsonPath("$.user.id").value(USER_ID))
                .andExpect(jsonPath("$.user.name").value("Ana"))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.user.createdAt").value("2026-10-22T10:00:00Z"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(userService).register(REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ana@example.com", "ana@example.com ", "  ana@example.com  "})
    @DisplayName("El registro acepta espacios alrededor del email antes de validarlo")
    void aceptaEmailConEspacios(String email) throws Exception {
        when(userService.register(REQUEST)).thenReturn(new AuthResponse(RESPONSE, "signed-token"));

        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"%s","password":"password123"}
                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("ana@example.com"));
        verify(userService).register(REQUEST);
    }

    @Test
    @DisplayName("Un campo inválido devuelve 400 antes de invocar el servicio")
    void rechazaRegistroInvalido() throws Exception {
        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"invalido","password":"password123"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.timestamp").isString());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Una contraseña corta devuelve 400 VALIDATION_ERROR sin invocar el servicio")
    void rechazaPasswordCorta() throws Exception {
        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"ana@example.com","password":"short"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("password"))
                .andExpect(jsonPath("$.details[0].message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Un campo obligatorio ausente devuelve el detalle de validación")
    void rechazaCampoObligatorioAusente() throws Exception {
        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"password123"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Un email duplicado devuelve 409 con el contrato de error")
    void rechazaEmailDuplicado() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenThrow(new DuplicateUserException());

        mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("La consulta devuelve 200 con los datos públicos del usuario")
    void consultaUsuarioExistente() throws Exception {
        when(userService.getById(USER_ID)).thenReturn(RESPONSE);

        mockMvc.perform(get(USERS_PATH + "/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-22T10:00:00Z"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(userService).getById(USER_ID);
    }

    @Test
    @DisplayName("Un usuario inexistente devuelve 404 con el contrato de error")
    void devuelveUsuarioNoEncontrado() throws Exception {
        when(userService.getById(USER_ID)).thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get(USERS_PATH + "/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }

}

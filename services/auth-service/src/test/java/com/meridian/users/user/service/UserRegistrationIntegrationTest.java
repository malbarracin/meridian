package com.meridian.users.user.service;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meridian.users.config.JwtConfig;
import com.meridian.users.config.SecurityConfig;
import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.exception.DuplicateUserException;
import com.meridian.users.user.exception.UserNotFoundException;
import com.meridian.users.user.mapper.UserMapper;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationIntegrationTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new SecurityConfig().passwordEncoder();
        jwtService = new JwtService(new JwtConfig("test-secret-key-for-unit-tests-only-32chars", 3600000));
        userService = new UserService(userRepository, passwordEncoder, Mappers.getMapper(UserMapper.class), jwtService);
    }

    @Test
    @DisplayName("Registra con email normalizado, hash BCrypt y fechas de creación")
    void registersNormalizedUserWithHashedPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("Ana", "  ANA@EXAMPLE.COM  ", "password123");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        Instant before = Instant.now();

        AuthResponse response = userService.register(request);

        verify(userRepository).findByEmail("ana@example.com");
        ArgumentCaptor<User> capturedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(capturedUser.capture());
        User savedUser = capturedUser.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("ana@example.com");
        assertThat(savedUser.getName()).isEqualTo("Ana");
        assertThat(savedUser.getPasswordHash()).startsWith("$2").isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getCreatedAt()).isBetween(before, Instant.now());
        assertThat(savedUser.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
        assertThat(response.user()).isEqualTo(new UserResponse("user-1", "Ana", "ana@example.com",
                savedUser.getCreatedAt()));
        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.isValid(response.token())).isTrue();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json).doesNotContain("password", savedUser.getPasswordHash());
        assertThat(request.toString()).doesNotContain(request.password(), request.email(), request.name());
    }

    @Test
    @DisplayName("Rechaza un email duplicado antes de persistir")
    void rejectsDuplicateNormalizedEmail() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(
                new RegisterRequest("Ana", " ANA@EXAMPLE.COM ", "password123")))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessage("Email ya registrado");
        verify(userRepository).findByEmail("ana@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Consulta un usuario y devuelve sus datos públicos")
    void returnsUserById() {
        User user = new User();
        user.setId("user-1");
        user.setName("Ana");
        user.setEmail("ana@example.com");
        user.setCreatedAt(Instant.parse("2026-10-22T12:00:00Z"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThat(userService.getById("user-1")).isEqualTo(
                new UserResponse("user-1", "Ana", "ana@example.com", user.getCreatedAt()));
    }

    @Test
    @DisplayName("Lanza la excepción de dominio cuando el usuario no existe")
    void rejectsMissingUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById("missing"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuario no encontrado por id: missing");
    }
}

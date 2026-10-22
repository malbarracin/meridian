package com.meridian.users.user.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.exception.DuplicateUserException;
import com.meridian.users.user.mapper.UserMapper;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final RegisterRequest REQUEST = new RegisterRequest("Ada", " ADA@EXAMPLE.COM ", "password123");
    private static final String PASSWORD_HASH = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Normaliza el email y persiste el hash generado por el encoder")
    void normalizaEmailYHasheaAntesDePersistir() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(REQUEST.password())).thenReturn(PASSWORD_HASH);
        User persistedUser = new User();
        persistedUser.setId("user-1");
        persistedUser.setEmail("ada@example.com");
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        UserResponse expected = new UserResponse("user-1", "Ada", "ada@example.com", Instant.now());
        when(userMapper.toResponse(persistedUser)).thenReturn(expected);
        when(jwtService.generateToken("user-1", "ada@example.com")).thenReturn("signed-token");

        AuthResponse response = userService.register(REQUEST);

        ArgumentCaptor<User> capturedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(capturedUser.capture());
        verify(userRepository).findByEmail("ada@example.com");
        verify(passwordEncoder).encode(REQUEST.password());
        verify(userMapper).toResponse(persistedUser);
        assertThat(capturedUser.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(capturedUser.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH)
                .isNotEqualTo(REQUEST.password());
        verify(jwtService).generateToken("user-1", "ada@example.com");
        assertThat(response.user()).isSameAs(expected);
        assertThat(response.token()).isEqualTo("signed-token").isNotBlank();
    }

    @Test
    @DisplayName("Rechaza el email duplicado sin invocar encoder ni mapper")
    void rechazaEmailExistente() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(REQUEST)).isInstanceOf(DuplicateUserException.class);

        verifyNoInteractions(passwordEncoder, userMapper, jwtService);
        verify(userRepository, never()).save(any(User.class));
    }
}

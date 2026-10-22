package com.meridian.users.user.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.meridian.users.user.dto.AuthResponse;
import com.meridian.users.user.dto.RegisterRequest;
import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.exception.DuplicateUserException;
import com.meridian.users.user.exception.UserNotFoundException;
import com.meridian.users.user.mapper.UserMapper;
import com.meridian.users.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Casos de uso de registro y consulta de usuarios. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    /**
     * Registra un usuario con email normalizado y contraseña hasheada.
     * @param request datos validados del registro
     * @return datos públicos del usuario persistido y JWT de acceso
     * @throws DuplicateUserException si el email normalizado ya existe
     */
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateUserException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Instant now = Instant.now();
        User user = new User();
        user.setName(request.name());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User savedUser = userRepository.save(user);
        UserResponse response = userMapper.toResponse(savedUser);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());
        return new AuthResponse(response, token);
    }

    /**
     * Consulta un usuario por su identificador.
     * @param id identificador del usuario
     * @return datos públicos del usuario encontrado
     * @throws UserNotFoundException si el usuario no existe
     */
    public UserResponse getById(String id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}

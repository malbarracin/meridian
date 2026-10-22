package com.meridian.users.user.mapper;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Convierte el usuario a respuesta pública sin passwordHash")
    void mapsUserToPublicResponseWithoutPasswordHash() throws Exception {
        User user = new User();
        user.setId("user-1");
        user.setName("Ana");
        user.setEmail("ana@example.com");
        user.setPasswordHash("$2a$10$secret-hash-value");
        user.setCreatedAt(Instant.parse("2026-10-22T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-10-22T12:00:00Z"));

        UserResponse response = userMapper.toResponse(user);

        assertThat(response).isEqualTo(new UserResponse(
                "user-1", "Ana", "ana@example.com", Instant.parse("2026-10-22T12:00:00Z")));
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json)
                .doesNotContain("password")
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$10$secret-hash-value");
    }
}

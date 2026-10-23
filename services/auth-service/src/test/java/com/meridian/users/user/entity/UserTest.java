package com.meridian.users.user.entity;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("toString no incluye passwordHash ni su valor")
    void toStringNoIncluyePasswordHash() {
        User user = new User();
        user.setId("user-1");
        user.setName("Ada");
        user.setEmail("ada@example.com");
        user.setPasswordHash("$2a$10$secret-hash-value");
        user.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-15T10:00:00Z"));

        String text = user.toString();

        assertThat(text).doesNotContain("passwordHash");
        assertThat(text).doesNotContain("$2a$10$secret-hash-value");
        assertThat(text).contains("user-1");
    }
}

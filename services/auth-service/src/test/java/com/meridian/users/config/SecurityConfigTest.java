package com.meridian.users.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("Expone un PasswordEncoder BCrypt sin cadena HTTP")
    void providesBCryptPasswordEncoder() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("password123");
        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("password123", hash)).isTrue();
        assertThat(encoder.matches("otra", hash)).isFalse();
    }
}

package com.meridian.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Configuración del hash de contraseñas del servicio. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** @return codificador BCrypt para las contraseñas de registro */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

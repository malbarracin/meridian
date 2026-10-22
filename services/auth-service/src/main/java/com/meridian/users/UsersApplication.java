package com.meridian.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.meridian.users.config.JwtConfig;

/** Punto de entrada del servicio de autenticación. */
@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
public class UsersApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersApplication.class, args);
    }
}

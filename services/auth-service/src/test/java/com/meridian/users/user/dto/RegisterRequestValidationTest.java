package com.meridian.users.user.dto;

import java.lang.reflect.RecordComponent;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("Acepta un registro válido con contraseña de al menos 8 caracteres")
    void acceptsValidRegisterRequest() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(new RegisterRequest("Ana", "ana@example.com", "password123"));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Rechaza email inválido, campos vacíos y contraseña corta")
    void rejectsInvalidRegisterRequest() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(new RegisterRequest(" ", "no-email", "short"));

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet()))
                .contains("name", "email", "password");
    }

    @Test
    @DisplayName("UserResponse no declara campo passwordHash")
    void userResponseHasNoPasswordHashComponent() {
        Set<String> components = Set.of(UserResponse.class.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(components).containsExactlyInAnyOrder("id", "name", "email", "createdAt");
        assertThat(components).doesNotContain("passwordHash", "password");
    }

    @Test
    @DisplayName("toString del registro no expone datos sensibles")
    void registerRequestToStringHidesSensitiveData() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "password123");

        assertThat(request.toString())
                .doesNotContain("Ana")
                .doesNotContain("ana@example.com")
                .doesNotContain("password123");
    }
}

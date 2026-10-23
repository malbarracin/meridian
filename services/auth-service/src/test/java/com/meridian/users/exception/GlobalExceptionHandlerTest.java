package com.meridian.users.exception;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Los errores de cuerpo incluyen el campo inválido")
    void includesInvalidBodyField() throws NoSuchMethodException {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new org.springframework.validation.FieldError("request", "name", "obligatorio"));
        var parameter = new MethodParameter(getClass().getDeclaredMethod("request", String.class), 0);
        var response = handler.handleValidation(new MethodArgumentNotValidException(parameter, binding));
        assertResponse(response, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
        assertThat(response.getBody().details()).containsExactly(new FieldError("name", "obligatorio"));
    }

    @Test
    @DisplayName("Las restricciones de parámetros incluyen el campo inválido")
    void includesConstraintViolation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new ValidatedRequest(""));
            var response = handler.handleConstraintViolation(new ConstraintViolationException(violations));
            assertResponse(response, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
            assertThat(response.getBody().details()).extracting(FieldError::field).containsExactly("name");
        }
    }

    @ParameterizedTest
    @MethodSource("domainErrors")
    @DisplayName("Los errores conservan el contrato y ocultan detalles internos")
    void mapsErrors(ResponseEntity<ErrorResponse> response, HttpStatus status, ErrorCode code) {
        assertResponse(response, status, code);
        assertThat(response.getBody().details()).isEmpty();
        assertThat(response.getBody().message()).doesNotContain("secret");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> domainErrors() {
        var handler = new GlobalExceptionHandler();
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        handler.handleBadRequest(new IllegalArgumentException("secret")),
                        HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST),
                org.junit.jupiter.params.provider.Arguments.of(
                        handler.handleNotFound(new NotFoundException("Recurso inexistente") { }),
                        HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND),
                org.junit.jupiter.params.provider.Arguments.of(
                        handler.handleConflict(new ConflictException("Conflicto de estado") { }),
                        HttpStatus.CONFLICT, ErrorCode.CONFLICT),
                org.junit.jupiter.params.provider.Arguments.of(
                        handler.handleDuplicateKey(new DuplicateKeyException("secret")),
                        HttpStatus.CONFLICT, ErrorCode.CONFLICT),
                org.junit.jupiter.params.provider.Arguments.of(
                        handler.handleUnexpected(new IllegalStateException("secret")),
                        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR));
    }

    @Test
    @DisplayName("La lista de detalles del error es inmutable")
    void protectsErrorDetails() {
        var response = new ErrorResponse(ErrorCode.BAD_REQUEST.name(), "Solicitud inválida", List.of(), Instant.now());
        assertThatThrownBy(() -> response.details().add(new FieldError("name", "obligatorio")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertResponse(ResponseEntity<ErrorResponse> response, HttpStatus status, ErrorCode code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(code.name());
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(Instant.now());
        assertThat(response.getBody().details()).isNotNull();
    }

    private void request(String name) {
    }

    private record ValidatedRequest(@NotBlank String name) {
    }
}

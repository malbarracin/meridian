package com.meridian.users.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.meridian.users.exception.ConflictException;
import com.meridian.users.exception.ErrorCode;
import com.meridian.users.exception.ErrorResponse;
import com.meridian.users.exception.GlobalExceptionHandler;
import com.meridian.users.exception.NotFoundException;
import com.meridian.users.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;

class UserDomainExceptionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DuplicateUserException extiende ConflictException y responde 409 CONFLICT")
    void mapsDuplicateUserToConflictContract() {
        DuplicateUserException exception = new DuplicateUserException();

        assertThat(exception).isInstanceOf(ConflictException.class);
        assertThat(exception.getMessage()).isEqualTo("Email ya registrado");

        var response = handler.handleConflict(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.CONFLICT.name());
        assertThat(body.message()).isEqualTo("Email ya registrado");
        assertThat(body.details()).isEmpty();
    }

    @Test
    @DisplayName("UserNotFoundException extiende NotFoundException y responde 404 NOT_FOUND")
    void mapsMissingUserToNotFoundContract() {
        UserNotFoundException exception = new UserNotFoundException("user-42");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("Usuario no encontrado por id: user-42");

        var response = handler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
        assertThat(body.message()).isEqualTo("Usuario no encontrado por id: user-42");
        assertThat(body.details()).isEmpty();
    }

    @Test
    @DisplayName("InvalidCredentialsException extiende UnauthorizedException y responde 401 UNAUTHORIZED")
    void mapsInvalidCredentialsToUnauthorizedContract() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        assertThat(exception).isInstanceOf(UnauthorizedException.class);
        assertThat(exception.getMessage()).isEqualTo("Credenciales inválidas");

        var response = handler.handleUnauthorized(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.UNAUTHORIZED.name());
        assertThat(body.message()).isEqualTo("Credenciales inválidas");
        assertThat(body.details()).isEmpty();
    }
}

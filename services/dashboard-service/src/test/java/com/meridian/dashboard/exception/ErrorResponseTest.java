package com.meridian.dashboard.exception;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Modelo público de ErrorResponse")
class ErrorResponseTest {

    @Test
    @DisplayName("Expone details inmodificable y no nulo")
    void copiesDetailsAsUnmodifiableList() {
        List<FieldError> mutable = new ArrayList<>();
        mutable.add(new FieldError("name", "obligatorio"));

        ErrorResponse response = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                "La solicitud tiene datos inválidos",
                mutable,
                Instant.parse("2026-10-22T12:00:00Z"));

        mutable.clear();
        assertThat(response.details()).hasSize(1);
        assertThatThrownBy(() -> response.details().add(new FieldError("other", "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

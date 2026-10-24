package com.meridian.dashboard.exception;

import lombok.Getter;

/** Parámetro fuera del catálogo admitido por un endpoint. */
@Getter
public class UnsupportedParameterException extends RuntimeException {

    private final String field;

    public UnsupportedParameterException(String field) {
        super("La solicitud tiene datos inválidos");
        this.field = field;
    }
}

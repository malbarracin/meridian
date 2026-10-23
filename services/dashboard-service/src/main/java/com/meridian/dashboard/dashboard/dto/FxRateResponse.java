package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Unidades de la moneda destino por una unidad de la moneda origen. */
@Schema(description = "Tipo de cambio entre dos divisas")
public record FxRateResponse(
        @Schema(description = "Moneda origen (ISO 4217)", example = "USD") String from,
        @Schema(description = "Moneda destino (ISO 4217)", example = "EUR") String to,
        @Schema(description = "Unidades de la moneda destino por una unidad de la origen", example = "0.92")
        BigDecimal rate) {
}

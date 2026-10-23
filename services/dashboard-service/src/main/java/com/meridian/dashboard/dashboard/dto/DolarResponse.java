package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cotización de compra y venta de un tipo de dólar, expresada en pesos argentinos. */
@Schema(description = "Cotización de compra y venta de un tipo de dólar en ARS")
public record DolarResponse(
        @Schema(description = "Tipo de dólar", example = "oficial") String tipo,
        @Schema(description = "Precio de compra en ARS", example = "1000.00") BigDecimal compra,
        @Schema(description = "Precio de venta en ARS", example = "1040.00") BigDecimal venta) {
}

package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Vela diaria: t es Unix en segundos; o/h/l/c son precios y v es volumen negociado. */
@Schema(description = "Vela OHLC diaria expresada con timestamp Unix en segundos")
public record OhlcPoint(
        @Schema(description = "Timestamp Unix en segundos", example = "1792627200") long t,
        @Schema(description = "Precio de apertura", example = "175.00") BigDecimal o,
        @Schema(description = "Precio máximo", example = "175.00") BigDecimal h,
        @Schema(description = "Precio mínimo", example = "175.00") BigDecimal l,
        @Schema(description = "Precio de cierre", example = "175.00") BigDecimal c,
        @Schema(description = "Volumen negociado", example = "0") BigDecimal v) {
}

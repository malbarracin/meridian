package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Último precio de una acción o ETF, expresado en la moneda indicada. */
@Schema(description = "Último precio de una acción o ETF")
public record EquityQuoteResponse(
        @Schema(description = "Ticker del instrumento", example = "AAPL") String ticker,
        @Schema(description = "Último precio", example = "175.00") BigDecimal price,
        @Schema(description = "Moneda del precio (ISO 4217)", example = "USD") String currency) {
}

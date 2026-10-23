package com.meridian.dashboard.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/** Serie OHLC inmutable de un ticker. */
@Schema(description = "Serie OHLC diaria de un instrumento")
public record CandleResponse(
        @Schema(description = "Ticker del instrumento", example = "AAPL") String ticker,
        @ArraySchema(arraySchema = @Schema(description = "Velas diarias del rango consultado"),
                schema = @Schema(implementation = OhlcPoint.class))
        List<OhlcPoint> ohlcPoints) {

    public CandleResponse {
        ohlcPoints = List.copyOf(ohlcPoints);
    }
}

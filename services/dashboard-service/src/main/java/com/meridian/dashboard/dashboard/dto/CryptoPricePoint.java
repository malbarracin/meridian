package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Punto histórico con fecha UTC y precio en USD. */
@Schema(description = "Punto de la serie histórica de una criptomoneda")
public record CryptoPricePoint(
        @Schema(description = "Fecha y hora UTC del punto", example = "2026-10-15T00:00:00Z")
        Instant timestamp,
        @Schema(description = "Precio en USD en ese momento", example = "67000") BigDecimal priceUsd) {
}

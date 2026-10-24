package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Fecha UTC y precio en USD; en bolsa se utiliza el precio de cierre. */
public record ChartPoint(
        @Schema(description = "Fecha UTC", example = "2026-10-22T00:00:00Z") Instant t,
        @Schema(description = "Precio en USD", example = "67000") BigDecimal v) {
}

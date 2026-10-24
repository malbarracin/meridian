package com.meridian.dashboard.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Serie histórica; degraded indica que no se pudo obtener una serie fresca del proveedor. */
public record ChartsResponse(
        @Schema(description = "Activo normalizado", example = "BTC") String asset,
        @Schema(description = "Ventana histórica", example = "7d") String range,
        @Schema(description = "Puntos ordenados por fecha") List<ChartPoint> points,
        @Schema(description = "Fallo del proveedor sin caché fresca", example = "false") boolean degraded) {

    public ChartsResponse {
        points = List.copyOf(points);
    }
}

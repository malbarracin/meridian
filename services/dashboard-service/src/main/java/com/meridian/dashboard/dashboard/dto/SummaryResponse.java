package com.meridian.dashboard.dashboard.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen público de mercados de Meridian.
 * <p>Protegido por JWT. Agrega dólar ARS, criptomonedas y bolsa con lista
 * de secciones degradadas.</p>
 *
 * @param brand marca del producto
 * @param generatedAt momento de generación de la respuesta
 * @param fx sección de dólar ARS con cotizaciones oficial/blue
 * @param crypto sección de criptomonedas
 * @param equities sección de acciones/índices
 * @param degraded lista de secciones sin datos frescos
 */
@Schema(description = "Resumen de mercados protegido por JWT")
public record SummaryResponse(
        @Schema(description = "Marca del producto", example = "Meridian") String brand,
        @Schema(description = "Momento de generación en UTC", example = "2026-10-22T10:05:00Z")
        Instant generatedAt,
        @Schema(description = "Dólar ARS (oficial/blue); no es Frankfurter") FxSection fx,
        @Schema(description = "Precios de criptomonedas indexados por id") CryptoSection crypto,
        @Schema(description = "Cotizaciones bursátiles indexadas por ticker") EquitiesSection equities,
        @Schema(description = "Secciones sin datos frescos: fx, crypto y/o equities")
        List<String> degraded) {
    public SummaryResponse {
        degraded = List.copyOf(degraded);
    }
}


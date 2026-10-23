package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Precio en USD y variación porcentual de las últimas 24 horas. */
@Schema(description = "Precio actual y variación de las últimas 24 horas de una criptomoneda")
public record CryptoResponse(
        @Schema(description = "Identificador de la moneda en CoinGecko", example = "bitcoin") String id,
        @Schema(description = "Símbolo de la moneda", example = "BTC") String symbol,
        @Schema(description = "Precio actual en USD", example = "67000") BigDecimal priceUsd,
        @Schema(description = "Variación porcentual de las últimas 24 horas", example = "2.5")
        BigDecimal change24h) {
}

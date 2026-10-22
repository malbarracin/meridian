package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Precio en USD y variación porcentual de las últimas 24 horas. */
public record CryptoResponse(String id, String symbol, BigDecimal priceUsd, BigDecimal change24h) {
}

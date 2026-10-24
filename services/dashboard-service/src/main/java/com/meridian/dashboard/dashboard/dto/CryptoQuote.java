package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Precio USD y variación porcentual de las últimas 24 horas. */
public record CryptoQuote(BigDecimal priceUsd, BigDecimal change24hPct) {
}


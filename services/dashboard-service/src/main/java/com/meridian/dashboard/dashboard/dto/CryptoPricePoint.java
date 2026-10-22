package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Punto histórico con fecha UTC y precio en USD. */
public record CryptoPricePoint(Instant timestamp, BigDecimal priceUsd) {
}

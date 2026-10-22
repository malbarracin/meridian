package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Último precio de una acción o ETF, expresado en la moneda indicada. */
public record EquityQuoteResponse(String ticker, BigDecimal price, String currency) {
}

package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Precio y moneda de una cotización bursátil. */
public record EquityQuote(BigDecimal price, String currency) {
}


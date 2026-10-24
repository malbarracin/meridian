package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Precio de compra y venta en ARS. */
public record RateQuote(BigDecimal compra, BigDecimal venta) {
}


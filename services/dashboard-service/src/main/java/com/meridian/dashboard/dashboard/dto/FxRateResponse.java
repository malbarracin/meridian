package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Unidades de la moneda destino por una unidad de la moneda origen. */
public record FxRateResponse(String from, String to, BigDecimal rate) {
}

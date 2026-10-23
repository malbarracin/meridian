package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Cotización de compra y venta de un tipo de dólar, expresada en pesos argentinos. */
public record DolarResponse(String tipo, BigDecimal compra, BigDecimal venta) {
}

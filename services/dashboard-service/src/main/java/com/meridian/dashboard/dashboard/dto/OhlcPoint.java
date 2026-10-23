package com.meridian.dashboard.dashboard.dto;

import java.math.BigDecimal;

/** Vela diaria: t es Unix en segundos; o/h/l/c son precios y v es volumen negociado. */
public record OhlcPoint(long t, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, BigDecimal v) {
}

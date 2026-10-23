package com.meridian.dashboard.dashboard.adapter;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Precio corriente del contrato de Finnhub. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuote(BigDecimal c) {
}

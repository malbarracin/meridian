package com.meridian.dashboard.dashboard.dto;

import java.util.Map;

/** Cotizaciones de dólar en ARS por tipo público. */
public record FxSection(Map<String, RateQuote> quotes) {
    public FxSection {
        quotes = Map.copyOf(quotes);
    }
}


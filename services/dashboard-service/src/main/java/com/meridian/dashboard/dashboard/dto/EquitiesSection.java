package com.meridian.dashboard.dashboard.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

/** Cotizaciones por ticker, sin envoltorio values en JSON. */
public record EquitiesSection(@JsonValue Map<String, EquityQuote> values) {
    public EquitiesSection {
        values = Map.copyOf(values);
    }
}


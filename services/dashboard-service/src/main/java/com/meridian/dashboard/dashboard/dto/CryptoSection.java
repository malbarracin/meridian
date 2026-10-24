package com.meridian.dashboard.dashboard.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

/** Precios por identificador de criptomoneda, sin envoltorio values en JSON. */
public record CryptoSection(@JsonValue Map<String, CryptoQuote> values) {
    public CryptoSection {
        values = Map.copyOf(values);
    }
}


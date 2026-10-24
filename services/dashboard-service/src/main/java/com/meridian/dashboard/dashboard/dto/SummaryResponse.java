package com.meridian.dashboard.dashboard.dto;

import java.time.Instant;
import java.util.List;

/** Resumen público de mercados de Meridian. */
public record SummaryResponse(String brand, Instant generatedAt, FxSection fx,
        CryptoSection crypto, EquitiesSection equities, List<String> degraded) {
    public SummaryResponse {
        degraded = List.copyOf(degraded);
    }
}


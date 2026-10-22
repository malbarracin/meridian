package com.meridian.dashboard.dashboard.adapter;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contrato externo de Frankfurter para una moneda base y sus tasas de cambio. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterRates(String base, Map<String, BigDecimal> rates) {
}

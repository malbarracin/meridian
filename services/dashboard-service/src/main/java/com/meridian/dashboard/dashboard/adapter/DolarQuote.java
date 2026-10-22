package com.meridian.dashboard.dashboard.adapter;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contrato externo de DolarApi; casa identifica el tipo de cotización. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DolarQuote(String casa, BigDecimal compra, BigDecimal venta) {
}

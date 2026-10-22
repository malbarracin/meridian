package com.meridian.dashboard.dashboard.adapter;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Arrays paralelos y estado devueltos por Finnhub. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubCandles(String s, List<Long> t, List<BigDecimal> o, List<BigDecimal> h,
        List<BigDecimal> l, List<BigDecimal> c, List<BigDecimal> v) {
}

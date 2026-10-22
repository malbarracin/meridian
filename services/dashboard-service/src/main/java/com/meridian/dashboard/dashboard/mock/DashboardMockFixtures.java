package com.meridian.dashboard.dashboard.mock;

import java.math.BigDecimal;
import java.util.List;

import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.dto.FxRateResponse;
import com.meridian.dashboard.dashboard.dto.CandleResponse;
import com.meridian.dashboard.dashboard.dto.EquityQuoteResponse;
import com.meridian.dashboard.dashboard.dto.OhlcPoint;

/** Datos de demostración estáticos; no representan cotizaciones actuales. */
public final class DashboardMockFixtures {

    private DashboardMockFixtures() {
    }

    /** @return cotizaciones ficticias de dólar oficial y blue en ARS. */
    public static List<DolarResponse> getDolar() {
        return List.of(
                new DolarResponse("oficial", new BigDecimal("1000.00"), new BigDecimal("1040.00")),
                new DolarResponse("blue", new BigDecimal("1200.00"), new BigDecimal("1220.00")));
    }

    /** @return tipos de cambio ficticios desde USD hacia EUR y BRL. */
    public static List<FxRateResponse> getFx() {
        return List.of(
                new FxRateResponse("USD", "EUR", new BigDecimal("0.92")),
                new FxRateResponse("USD", "BRL", new BigDecimal("5.10")));
    }

    /** @return precios demo fijos en USD: AAPL 175, MSFT 415 y SPY 450. */
    public static List<EquityQuoteResponse> getEquities() {
        return List.of(
                new EquityQuoteResponse("AAPL", new BigDecimal("175.00"), "USD"),
                new EquityQuoteResponse("MSFT", new BigDecimal("415.00"), "USD"),
                new EquityQuoteResponse("SPY", new BigDecimal("450.00"), "USD"));
    }

    /** @return una vela sintética por ticker, fija al 2026-10-22 00:00 UTC, sin volumen real. */
    public static List<CandleResponse> getEquityCandles() {
        return getEquities().stream().map(quote -> new CandleResponse(quote.ticker(), List.of(
                new OhlcPoint(1792627200L, quote.price(), quote.price(), quote.price(), quote.price(),
                        BigDecimal.ZERO)))).toList();
    }
}

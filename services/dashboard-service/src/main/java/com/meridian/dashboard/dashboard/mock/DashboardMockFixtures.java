package com.meridian.dashboard.dashboard.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import com.meridian.dashboard.dashboard.dto.CandleResponse;
import com.meridian.dashboard.dashboard.dto.CryptoPricePoint;
import com.meridian.dashboard.dashboard.dto.CryptoResponse;
import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.dto.EquityQuoteResponse;
import com.meridian.dashboard.dashboard.dto.FxRateResponse;
import com.meridian.dashboard.dashboard.dto.NewsArticleResponse;
import com.meridian.dashboard.dashboard.dto.OhlcPoint;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Datos de demostración estáticos; no representan cotizaciones actuales. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DashboardMockFixtures {

    private static final Instant HISTORY_START = Instant.parse("2026-10-15T00:00:00Z");
    private static final int DAYS = 7;
    private static final long SECONDS_PER_DAY = 86400;

    public static List<CryptoResponse> getCrypto() {
        return List.of(
                new CryptoResponse("bitcoin", "BTC", new BigDecimal("67000"), new BigDecimal("2.5")),
                new CryptoResponse("ethereum", "ETH", new BigDecimal("3500"), new BigDecimal("-1.2")));
    }

    /** Serie diaria fija de BTC, mantenida como fixture por defecto. */
    public static List<CryptoPricePoint> getCryptoHistory() {
        return getCryptoHistory("bitcoin");
    }

    /** Siete puntos diarios estáticos para la moneda solicitada. */
    public static List<CryptoPricePoint> getCryptoHistory(String coinId) {
        BigDecimal price = "bitcoin".equals(coinId) ? new BigDecimal("67000") : new BigDecimal("3500");
        return IntStream.rangeClosed(0, DAYS)
                .mapToObj(day -> new CryptoPricePoint(HISTORY_START.plusSeconds(day * SECONDS_PER_DAY), price))
                .toList();
    }

    /** @return noticias ficticias estáticas para la demostración offline. */
    public static List<NewsArticleResponse> getNews() {
        return List.of(
                new NewsArticleResponse("demo-ai", "AI research demo", "Static AI news fixture", "Meridian Demo",
                        Instant.parse("2026-01-01T12:00:00Z"), "https://example.com/demo-ai", "ai-tech"),
                new NewsArticleResponse("demo-tech", "Technology demo", "Static technology news fixture",
                        "Meridian Demo", Instant.parse("2026-01-02T12:00:00Z"),
                        "https://example.com/demo-tech", "ai-tech"));
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

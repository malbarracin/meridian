package com.meridian.dashboard.dashboard.dto;

import java.util.List;

/**
 * Datos agregados del dashboard. cryptoHistory contiene las series de BTC y ETH.
 * degraded identifica secciones sin datos frescos, incluidas las servidas desde cache vencida.
 */
public record DashboardResponse(
        List<DolarResponse> dolar,
        List<FxRateResponse> fx,
        List<CryptoResponse> crypto,
        List<CryptoPricePoint> cryptoHistory,
        List<EquityQuoteResponse> equities,
        List<CandleResponse> equityCandles,
        List<NewsArticleResponse> news,
        List<String> degraded) {

    public DashboardResponse {
        dolar = List.copyOf(dolar);
        fx = List.copyOf(fx);
        crypto = List.copyOf(crypto);
        cryptoHistory = List.copyOf(cryptoHistory);
        equities = List.copyOf(equities);
        equityCandles = List.copyOf(equityCandles);
        news = List.copyOf(news);
        degraded = List.copyOf(degraded);
    }
}

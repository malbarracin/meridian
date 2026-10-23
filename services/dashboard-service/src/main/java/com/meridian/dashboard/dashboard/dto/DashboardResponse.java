package com.meridian.dashboard.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos agregados del dashboard.
 *
 * <p>Cada lista corresponde a una sección independiente y puede quedar vacía si el proveedor
 * falla. {@code degraded} identifica las secciones sin datos frescos, incluidas las servidas
 * desde cache vencida; nunca es {@code null}.</p>
 */
@Schema(description = "Datos agregados de mercado y noticias para el dashboard")
public record DashboardResponse(
        @ArraySchema(arraySchema = @Schema(description = "Cotizaciones de dólar oficial y blue en ARS"),
                schema = @Schema(implementation = DolarResponse.class))
        List<DolarResponse> dolar,
        @ArraySchema(arraySchema = @Schema(description = "Tipos de cambio internacionales USD→EUR y USD→BRL"),
                schema = @Schema(implementation = FxRateResponse.class))
        List<FxRateResponse> fx,
        @ArraySchema(arraySchema = @Schema(description = "Precio actual y variación 24h de BTC y ETH en USD"),
                schema = @Schema(implementation = CryptoResponse.class))
        List<CryptoResponse> crypto,
        @ArraySchema(arraySchema = @Schema(description = "Serie de precios de los últimos 7 días de BTC y ETH"),
                schema = @Schema(implementation = CryptoPricePoint.class))
        List<CryptoPricePoint> cryptoHistory,
        @ArraySchema(arraySchema = @Schema(description = "Últimos precios de AAPL, MSFT y SPY"),
                schema = @Schema(implementation = EquityQuoteResponse.class))
        List<EquityQuoteResponse> equities,
        @ArraySchema(arraySchema = @Schema(description = "Velas OHLC diarias por ticker"),
                schema = @Schema(implementation = CandleResponse.class))
        List<CandleResponse> equityCandles,
        @ArraySchema(arraySchema = @Schema(description = "Noticias de IA y tecnología normalizadas"),
                schema = @Schema(implementation = NewsArticleResponse.class))
        List<NewsArticleResponse> news,
        @ArraySchema(arraySchema = @Schema(description = "Secciones sin datos frescos",
                example = "[\"dolar\", \"news\"]"),
                schema = @Schema(implementation = String.class, example = "dolar"))
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

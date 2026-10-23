package com.meridian.dashboard.dashboard.adapter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.dto.CandleResponse;
import com.meridian.dashboard.dashboard.dto.EquityQuoteResponse;
import com.meridian.dashboard.dashboard.dto.OhlcPoint;
import com.meridian.dashboard.dashboard.mapper.EquityMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

/** Consulta Finnhub; propaga fallos al agregador para degradar equities o equityCandles por separado. */
@Component
public class FinnhubAdapter {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HISTORY_WINDOW = Duration.ofDays(7);
    private static final List<String> TICKERS = List.of("AAPL", "MSFT", "SPY");

    private final WebClient webClient;
    private final EquityMapper mapper;
    private final String apiKey;
    private final boolean useMock;

    public FinnhubAdapter(@Qualifier("finnhubWebClient") WebClient webClient, EquityMapper mapper,
            @Value("${dashboard.providers.finnhub.api-key:}") String apiKey,
            @Value("${dashboard.use-mock:false}") boolean useMock) {
        this.webClient = Objects.requireNonNull(webClient);
        this.mapper = Objects.requireNonNull(mapper);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.useMock = useMock;
    }

    /** @return quotes de AAPL, MSFT y SPY; sin key o en modo mock devuelve fixtures. */
    public List<EquityQuoteResponse> fetchQuotes() {
        if (useMock || apiKey.isBlank()) {
            return DashboardMockFixtures.getEquities();
        }
        return TICKERS.stream().map(this::fetchQuote).toList();
    }

    /** @return velas de los tres tickers en los últimos siete días, o fixtures estáticas en modo demo. */
    public List<CandleResponse> fetchCandles() {
        if (useMock || apiKey.isBlank()) {
            return DashboardMockFixtures.getEquityCandles();
        }
        Instant to = Instant.now();
        return fetchCandles(to.minus(HISTORY_WINDOW).getEpochSecond(), to.getEpochSecond());
    }

    /** @return velas de los tres tickers en el intervalo Unix inclusivo indicado. */
    public List<CandleResponse> fetchCandles(long from, long to) {
        return TICKERS.stream().map(ticker -> fetchCandles(ticker, from, to)).toList();
    }

    /** @return serie de un ticker durante los últimos siete días, o su fixture estática. */
    public CandleResponse fetchCandles(String ticker) {
        if (useMock || apiKey.isBlank()) {
            return fetchCandles(ticker, 0, Long.MAX_VALUE);
        }
        Instant to = Instant.now();
        return fetchCandles(ticker, to.minus(HISTORY_WINDOW).getEpochSecond(), to.getEpochSecond());
    }

    /** @return serie del ticker en el intervalo Unix inclusivo; los fallos se propagan a equityCandles. */
    public CandleResponse fetchCandles(String ticker, long from, long to) {
        if (!TICKERS.contains(ticker) || from < 0 || from > to) {
            throw new IllegalArgumentException("Ticker o rango de velas inválido");
        }
        if (useMock || apiKey.isBlank()) {
            CandleResponse fixture = DashboardMockFixtures.getEquityCandles().stream()
                    .filter(series -> series.ticker().equals(ticker)).findFirst().orElseThrow();
            return mapper.toCandles(ticker, fixture.ohlcPoints().stream()
                    .filter(point -> point.t() >= from && point.t() <= to).toList());
        }
        FinnhubCandles candles = webClient.get()
                .uri(builder -> builder.path("/api/v1/stock/candle").queryParam("symbol", ticker)
                        .queryParam("resolution", "D").queryParam("from", from).queryParam("to", to)
                        .queryParam("token", apiKey).build())
                .retrieve().bodyToMono(FinnhubCandles.class).timeout(REQUEST_TIMEOUT).block();
        if (candles == null) {
            throw new IllegalStateException("Respuesta de velas vacía");
        }
        if ("no_data".equals(candles.s())) {
            return mapper.toCandles(ticker, List.of());
        }
        validateCandles(candles);
        List<OhlcPoint> points = IntStream.range(0, candles.t().size())
                .filter(index -> candles.t().get(index) >= from && candles.t().get(index) <= to)
                .mapToObj(index -> mapper.toPoint(candles.t().get(index), candles.o().get(index),
                        candles.h().get(index), candles.l().get(index), candles.c().get(index), candles.v().get(index)))
                .toList();
        return mapper.toCandles(ticker, points);
    }

    private EquityQuoteResponse fetchQuote(String ticker) {
        FinnhubQuote quote = webClient.get()
                .uri(builder -> builder.path("/api/v1/quote").queryParam("symbol", ticker)
                        .queryParam("token", apiKey).build())
                .retrieve().bodyToMono(FinnhubQuote.class).timeout(REQUEST_TIMEOUT).block();
        if (quote == null || quote.c() == null || quote.c().signum() <= 0) {
            throw new IllegalStateException("Cotización de bolsa incompleta");
        }
        return mapper.toQuote(ticker, quote);
    }

    private void validateCandles(FinnhubCandles candles) {
        if (!"ok".equals(candles.s()) || candles.t() == null || candles.t().contains(null)) {
            throw new IllegalStateException("Estado o fechas de velas inválidos");
        }
        for (List<?> values : java.util.Arrays.asList(candles.o(), candles.h(), candles.l(),
                candles.c(), candles.v())) {
            if (values == null || values.size() != candles.t().size() || values.contains(null)) {
                throw new IllegalStateException("Arrays de velas incompletos");
            }
        }
    }
}

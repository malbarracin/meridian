package com.meridian.dashboard.dashboard.adapter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import com.meridian.dashboard.dashboard.mapper.EquityMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Adapter de bolsa Finnhub")
class FinnhubAdapterTest {

    private MockWebServer server;

    @BeforeEach
    void iniciarProveedor() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void cerrarProveedor() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Consulta los tres tickers con la clave y normaliza los precios en USD")
    void consultaQuotesConClave() throws InterruptedException {
        server.enqueue(json("{\"c\":175.00}"));
        server.enqueue(json("{\"c\":415.00}"));
        server.enqueue(json("{\"c\":450.00}"));

        var quotes = adapter("test-key", false).fetchQuotes();

        assertThat(quotes).isEqualTo(DashboardMockFixtures.getEquities());
        for (String ticker : new String[]{"AAPL", "MSFT", "SPY"}) {
            var request = server.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/api/v1/quote?symbol=" + ticker + "&token=test-key");
        }
        assertThatThrownBy(quotes::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Sin clave devuelve precios y velas estáticas sin HTTP")
    void sirveFixturesSinClave() {
        var adapter = adapter("  ", false);

        assertThat(adapter.fetchQuotes()).isEqualTo(DashboardMockFixtures.getEquities());
        assertThat(adapter.fetchCandles()).isEqualTo(DashboardMockFixtures.getEquityCandles());
        assertThat(adapter.fetchCandles("AAPL")).isEqualTo(DashboardMockFixtures.getEquityCandles().getFirst());
        assertThat(adapter.fetchCandles("AAPL", 100, 200).ohlcPoints()).isEmpty();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("El modo mock evita HTTP incluso con una clave configurada")
    void sirveFixturesEnModoMock() {
        var adapter = adapter("test-key", true);

        assertThat(adapter.fetchQuotes()).isEqualTo(DashboardMockFixtures.getEquities());
        assertThat(adapter.fetchCandles()).isEqualTo(DashboardMockFixtures.getEquityCandles());
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Mapea arrays OHLCV y limita los puntos al rango Unix solicitado")
    void consultaVelasEnRango() throws InterruptedException {
        server.enqueue(json("""
                {"s":"ok","t":[100,200,300],"o":[10,11,12],"h":[14,15,16],
                 "l":[8,9,10],"c":[12,13,14],"v":[1000,2000,3000]}
                """));

        var series = adapter("test-key", false).fetchCandles("AAPL", 150, 250);

        assertThat(series.ticker()).isEqualTo("AAPL");
        assertThat(series.ohlcPoints()).hasSize(1);
        var point = series.ohlcPoints().getFirst();
        assertThat(point.t()).isEqualTo(200);
        assertThat(point.o()).isEqualByComparingTo("11");
        assertThat(point.h()).isEqualByComparingTo("15");
        assertThat(point.l()).isEqualByComparingTo("9");
        assertThat(point.c()).isEqualByComparingTo("13");
        assertThat(point.v()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(server.takeRequest(1, TimeUnit.SECONDS).getPath()).isEqualTo(
                "/api/v1/stock/candle?symbol=AAPL&resolution=D&from=150&to=250&token=test-key");
        assertThatThrownBy(series.ohlcPoints()::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Sin operaciones devuelve series vacías para los tres tickers")
    void aceptaNoData() {
        server.enqueue(json("{\"s\":\"no_data\"}"));
        server.enqueue(json("{\"s\":\"no_data\"}"));
        server.enqueue(json("{\"s\":\"no_data\"}"));

        var series = adapter("key", false).fetchCandles(100, 200);

        assertThat(series).extracting(candle -> candle.ticker()).containsExactly("AAPL", "MSFT", "SPY");
        assertThat(series).allSatisfy(candle -> assertThat(candle.ohlcPoints()).isEmpty());
    }

    @Test
    @DisplayName("Propaga fallos HTTP de cada sección para permitir degradación independiente")
    void propagaErroresHttp() {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse().setResponseCode(403));

        assertThatThrownBy(() -> adapter("key", false).fetchQuotes())
                .isInstanceOf(WebClientResponseException.class);
        assertThatThrownBy(() -> adapter("key", false).fetchCandles("SPY"))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    @DisplayName("Interrumpe quotes a los cinco segundos y propaga el timeout para degraded")
    void propagaTimeoutQuotes() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        long started = System.nanoTime();

        assertThatThrownBy(() -> adapter("key", false).fetchQuotes())
                .hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(4500L, 8000L);
    }

    @Test
    @DisplayName("Interrumpe velas a los cinco segundos y propaga el timeout para degraded")
    void propagaTimeoutVelas() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThatThrownBy(() -> adapter("key", false).fetchCandles("AAPL"))
                .hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    @DisplayName("Rechaza quotes vacías, sin precio y con precio cero")
    void rechazaQuotesInvalidas() {
        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(json("{}"));
        server.enqueue(json("{\"c\":0}"));

        var adapter = adapter("key", false);
        assertThatThrownBy(adapter::fetchQuotes).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(adapter::fetchQuotes).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(adapter::fetchQuotes).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza velas vacías, estados inválidos y arrays incompletos")
    void rechazaVelasInvalidas() {
        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(json("{\"s\":\"error\"}"));
        server.enqueue(json("{\"s\":\"ok\",\"t\":[100],\"o\":[]}"));

        var adapter = adapter("key", false);
        assertThatThrownBy(() -> adapter.fetchCandles("AAPL")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.fetchCandles("AAPL")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.fetchCandles("AAPL")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza ticker desconocido y rango invertido sin HTTP")
    void rechazaParametrosInvalidos() {
        var adapter = adapter("key", false);

        assertThatThrownBy(() -> adapter.fetchCandles("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.fetchCandles("AAPL", 200, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    private FinnhubAdapter adapter(String key, boolean useMock) {
        return new FinnhubAdapter(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Mappers.getMapper(EquityMapper.class), key, useMock);
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}

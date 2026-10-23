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

import com.meridian.dashboard.dashboard.dto.FxRateResponse;
import com.meridian.dashboard.dashboard.mapper.CurrencyMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Adapter FrankfurterAdapter")
class FrankfurterAdapterTest {

    private MockWebServer server;
    private FrankfurterAdapter adapter;

    @BeforeEach
    void configurarProveedor() throws IOException {
        server = new MockWebServer();
        server.start();
        adapter = createAdapter(false);
    }

    @AfterEach
    void cerrarProveedor() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Mapea las cotizaciones requeridas y llama a la ruta del proveedor")
    void mapeaCotizacionesRequeridas() throws InterruptedException {
        server.enqueue(jsonResponse("""
                {"amount":1,"base":"USD","date":"2026-10-22","rates":{"EUR":0.92,"BRL":5.10,"GBP":0.80}}
                """));

        var quotes = adapter.fetchFx();

        assertThat(quotes).containsExactly(
                new FxRateResponse("USD", "EUR", new BigDecimal("0.92")),
                new FxRateResponse("USD", "BRL", new BigDecimal("5.10")));
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/latest?from=USD&to=EUR,BRL");
        assertThatThrownBy(() -> quotes.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Devuelve fixtures sin llamadas HTTP en modo mock")
    void devuelveFixturesSinHttp() {
        var quotes = createAdapter(true).fetchFx();

        assertThat(quotes).isEqualTo(DashboardMockFixtures.getFx());
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Propaga el error HTTP para que el agregador degrade la sección")
    void propagaErrorDelProveedor() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(adapter::fetchFx).isInstanceOf(WebClientResponseException.class);
    }

    @Test
    @DisplayName("Interrumpe una respuesta sin datos al alcanzar cinco segundos")
    void propagaTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        long started = System.nanoTime();

        assertThatThrownBy(adapter::fetchFx).hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(4500L, 8000L);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza una respuesta vacía en vez de devolver null")
    void rechazaRespuestaVacia() {
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThatThrownBy(adapter::fetchFx).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza cotizaciones incompletas")
    void rechazaCotizacionesIncompletas() {
        server.enqueue(jsonResponse("{\"base\":\"USD\",\"rates\":{}}"));

        assertThatThrownBy(adapter::fetchFx).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza cuando falta la tasa BRL requerida")
    void rechazaCuandoFaltaTasaBrl() {
        server.enqueue(jsonResponse("""
                {"amount":1,"base":"USD","date":"2026-10-22","rates":{"EUR":0.92}}
                """));

        assertThatThrownBy(adapter::fetchFx)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requeridos");
    }

    private FrankfurterAdapter createAdapter(boolean useMock) {
        return new FrankfurterAdapter(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Mappers.getMapper(CurrencyMapper.class), useMock);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}


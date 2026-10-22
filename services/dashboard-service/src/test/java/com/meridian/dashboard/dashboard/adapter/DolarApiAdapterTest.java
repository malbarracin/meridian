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

import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.mapper.CurrencyMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Adapter DolarApiAdapter")
class DolarApiAdapterTest {

    private MockWebServer server;
    private DolarApiAdapter adapter;

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
                [{"casa":"oficial","nombre":"Oficial","compra":1000.50,"venta":1040.75},
                 {"casa":"blue","compra":1200,"venta":1220},
                 {"casa":"bolsa","compra":1100,"venta":1120}]
                """));

        var quotes = adapter.fetchDolar();

        assertThat(quotes).containsExactly(
                new DolarResponse("oficial", new BigDecimal("1000.50"), new BigDecimal("1040.75")),
                new DolarResponse("blue", new BigDecimal("1200"), new BigDecimal("1220")));
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/dolares");
        assertThatThrownBy(() -> quotes.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Devuelve fixtures sin llamadas HTTP en modo mock")
    void devuelveFixturesSinHttp() {
        var quotes = createAdapter(true).fetchDolar();

        assertThat(quotes).isEqualTo(DashboardMockFixtures.getDolar());
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Propaga el error HTTP para que el agregador degrade la sección")
    void propagaErrorDelProveedor() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(adapter::fetchDolar).isInstanceOf(WebClientResponseException.class);
    }

    @Test
    @DisplayName("Interrumpe una respuesta sin datos al alcanzar cinco segundos")
    void propagaTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        long started = System.nanoTime();

        assertThatThrownBy(adapter::fetchDolar).hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(4500L, 8000L);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza una respuesta vacía en vez de devolver null")
    void rechazaRespuestaVacia() {
        server.enqueue(new MockResponse().setResponseCode(204));

        assertThatThrownBy(adapter::fetchDolar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza cotizaciones incompletas")
    void rechazaCotizacionesIncompletas() {
        server.enqueue(jsonResponse("[{\"casa\":\"oficial\"}]"));

        assertThatThrownBy(adapter::fetchDolar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza cuando falta el tipo blue requerido")
    void rechazaCuandoFaltaTipoBlue() {
        server.enqueue(jsonResponse("""
                [{"casa":"oficial","compra":1000,"venta":1040},
                 {"casa":"bolsa","compra":1100,"venta":1120}]
                """));

        assertThatThrownBy(adapter::fetchDolar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requeridas");
    }

    private DolarApiAdapter createAdapter(boolean useMock) {
        return new DolarApiAdapter(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Mappers.getMapper(CurrencyMapper.class), useMock);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}


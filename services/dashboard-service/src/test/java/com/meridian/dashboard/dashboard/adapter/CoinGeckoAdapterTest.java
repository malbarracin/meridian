package com.meridian.dashboard.dashboard.adapter;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.mapper.CryptoMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CoinGecko: precios, historia y aislamiento de fallos")
class CoinGeckoAdapterTest {

    private static final String PRICES = """
            {"bitcoin":{"usd":67000.12,"usd_24h_change":2.5},
             "ethereum":{"usd":3500.42,"usd_24h_change":-1.2}}
            """;
    private static final String HISTORY = """
            {"prices":[[1760572800000,67000.12],[1761177600000,68000.45]],
             "market_caps":[],"total_volumes":[]}
            """;
    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");
    private MockWebServer server;
    private Clock clock;
    private MockEnvironment environment;
    private CoinGeckoAdapter adapter;

    @BeforeEach
    void configureProvider() throws Exception {
        server = new MockWebServer();
        server.start();
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        environment = new MockEnvironment().withProperty("dashboard.cache.ttl-seconds", "1");
        adapter = createAdapter();
    }

    @AfterEach
    void closeProvider() throws Exception {
        server.shutdown();
    }

    @Test
    @DisplayName("Mapea precio y variación de BTC y ETH con precisión decimal")
    void returnsBitcoinAndEthereumPrices() throws Exception {
        enqueue(PRICES);

        var prices = adapter.fetchPrices();

        assertThat(prices).extracting("id").containsExactly("bitcoin", "ethereum");
        assertThat(prices).extracting("symbol").containsExactly("BTC", "ETH");
        assertThat(prices.getFirst().priceUsd()).isEqualByComparingTo("67000.12");
        assertThat(prices.getFirst().change24h()).isEqualByComparingTo("2.5");
        assertThat(prices.getLast().priceUsd()).isEqualByComparingTo("3500.42");
        assertThat(prices.getLast().change24h()).isEqualByComparingTo("-1.2");
        assertThat(server.takeRequest().getPath()).isEqualTo(
                "/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true");
        assertThatThrownBy(prices::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Solicita siete días para cada moneda y convierte epoch milisegundos a UTC")
    void returnsSevenDayHistoryForEachCoin() throws Exception {
        enqueue(HISTORY);
        enqueue(HISTORY);

        var bitcoin = adapter.fetchHistory("bitcoin");
        var ethereum = adapter.fetchHistory("ethereum");

        assertThat(bitcoin).hasSize(2);
        assertThat(bitcoin.getFirst().timestamp()).isEqualTo(Instant.ofEpochMilli(1760572800000L));
        assertThat(bitcoin.getFirst().priceUsd()).isEqualByComparingTo("67000.12");
        assertThat(ethereum).isEqualTo(bitcoin);
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=7");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/v3/coins/ethereum/market_chart?vs_currency=usd&days=7");
        assertThat(adapter.fetchHistory("bitcoin")).isSameAs(bitcoin);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Aplica TTL mínimo de sesenta segundos y renueva al expirar")
    void clampsTtlAndRefreshesAtExpiry() {
        enqueue(PRICES);
        enqueue(PRICES);
        var prices = adapter.fetchPrices();

        when(clock.instant()).thenReturn(NOW.plusSeconds(59));
        assertThat(adapter.fetchPrices()).isSameAs(prices);
        assertThat(server.getRequestCount()).isEqualTo(1);

        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        assertThat(adapter.fetchPrices()).isEqualTo(prices);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Respeta TTL configurado superior al mínimo")
    void respectsLongerConfiguredTtl() {
        environment.withProperty("dashboard.cache.ttl-seconds", "120");
        adapter = createAdapter();
        enqueue(PRICES);
        adapter.fetchPrices();

        when(clock.instant()).thenReturn(NOW.plusSeconds(119));

        adapter.fetchPrices();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Propaga 5xx con la sección afectada y permite reintentar después")
    void identifiesFailuresWithoutCachingThem() {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(500));
        enqueue(PRICES);

        assertThatThrownBy(adapter::fetchPrices).isInstanceOfSatisfying(CoinGeckoException.class,
                exception -> {
                    assertThat(exception.getSection()).isEqualTo("crypto");
                    assertThat(exception.getCause()).isNotNull();
                });
        assertThatThrownBy(() -> adapter.fetchHistory("ethereum"))
                .isInstanceOfSatisfying(CoinGeckoException.class,
                        exception -> assertThat(exception.getSection()).isEqualTo("cryptoHistory"));
        assertThat(adapter.fetchPrices()).hasSize(2);
    }

    @Test
    @DisplayName("El modo demo devuelve fixtures de ambas monedas sin HTTP")
    void servesFixturesOffline() {
        environment.withProperty("dashboard.use-mock", "true");
        adapter = createAdapter();

        assertThat(adapter.fetchPrices()).isEqualTo(DashboardMockFixtures.getCrypto());
        assertThat(adapter.fetchHistory("bitcoin")).isEqualTo(DashboardMockFixtures.getCryptoHistory());
        assertThat(adapter.fetchHistory("ethereum"))
                .isEqualTo(DashboardMockFixtures.getCryptoHistory("ethereum"));
        assertThat(adapter.fetchHistory("bitcoin").getFirst().priceUsd()).isEqualByComparingTo("67000");
        assertThat(adapter.fetchHistory("ethereum").getFirst().priceUsd()).isEqualByComparingTo("3500");
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Interrumpe la llamada completa a los cinco segundos")
    void timesOutAtFiveSeconds() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        long started = System.nanoTime();

        assertThatThrownBy(adapter::fetchPrices).isInstanceOfSatisfying(CoinGeckoException.class,
                exception -> assertThat(exception.getSection()).isEqualTo("crypto"));

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(4500L, 8000L);
    }

    @Test
    @DisplayName("Rechaza monedas no soportadas sin llamar al proveedor")
    void rejectsUnsupportedCoin() {
        assertThatThrownBy(() -> adapter.fetchHistory("dogecoin")).isInstanceOf(IllegalArgumentException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("No cachea respuestas vacías ni precios incompletos")
    void rejectsInvalidPriceResponses() {
        server.enqueue(new MockResponse().setResponseCode(204));
        enqueue("{}");
        enqueue("{\"bitcoin\":{\"usd\":null,\"usd_24h_change\":2}}");

        assertThatThrownBy(adapter::fetchPrices).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(adapter::fetchPrices).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(adapter::fetchPrices).isInstanceOf(CoinGeckoException.class);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Rechaza series vacías, estructuras inválidas y puntos incompletos")
    void rejectsInvalidHistoryResponses() {
        enqueue("{\"prices\":[]}");
        enqueue("{\"prices\":{}}");
        enqueue("{\"prices\":[[1]]}");
        enqueue("{\"prices\":[[\"invalid\",2]]}");
        enqueue("{\"prices\":[[1,null]]}");

        assertThatThrownBy(() -> adapter.fetchHistory("bitcoin")).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(() -> adapter.fetchHistory("bitcoin")).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(() -> adapter.fetchHistory("bitcoin")).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(() -> adapter.fetchHistory("bitcoin")).isInstanceOf(CoinGeckoException.class);
        assertThatThrownBy(() -> adapter.fetchHistory("bitcoin")).isInstanceOf(CoinGeckoException.class);
    }

    @Test
    @DisplayName("El constructor de Spring usa reloj UTC y TTL por defecto")
    void constructsWithProductionClock() {
        var production = new CoinGeckoAdapter(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Mappers.getMapper(CryptoMapper.class), new MockEnvironment());
        enqueue(PRICES);

        assertThat(production.fetchPrices()).hasSize(2);
    }

    private CoinGeckoAdapter createAdapter() {
        return new CoinGeckoAdapter(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Mappers.getMapper(CryptoMapper.class), environment, clock);
    }

    private void enqueue(String body) {
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody(body));
    }
}

package com.meridian.dashboard.dashboard.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meridian.dashboard.dashboard.adapter.CoinGeckoAdapter;
import com.meridian.dashboard.dashboard.adapter.DolarApiAdapter;
import com.meridian.dashboard.dashboard.adapter.FinnhubAdapter;
import com.meridian.dashboard.dashboard.adapter.FrankfurterAdapter;
import com.meridian.dashboard.dashboard.adapter.NewsAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Agregación paralela del dashboard")
class DashboardServiceTest {

    @Mock private DolarApiAdapter dolar;
    @Mock private FrankfurterAdapter fx;
    @Mock private CoinGeckoAdapter crypto;
    @Mock private FinnhubAdapter equities;
    @Mock private NewsAdapter news;
    private ExecutorService executor;
    private DashboardService service;

    @BeforeEach
    void preparaServicio() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new DashboardService(dolar, crypto, equities, new DashboardCacheService(60), executor);
        when(dolar.fetchDolar()).thenReturn(DashboardMockFixtures.getDolar());
        when(crypto.fetchPrices()).thenReturn(DashboardMockFixtures.getCrypto());
        when(equities.fetchQuotes()).thenReturn(DashboardMockFixtures.getEquities());
    }

    @AfterEach
    void cierraExecutor() {
        executor.close();
    }

    @Test
    @DisplayName("Agrega todos los adapters y reutiliza los datos en la segunda consulta")
    void agregaYReutilizaCache() {
        var response = service.getSummary();
        assertThat(response.degraded()).isEmpty();
        assertThat(response.fx()).isEqualTo(DashboardMockFixtures.getSummaryFx());
        assertThat(response.fx().quotes()).containsOnlyKeys("usd_oficial", "usd_blue");
        assertThat(response.brand()).isEqualTo("Meridian");
        assertThat(response.brand()).doesNotContainIgnoringCase("Soma").isNotEqualTo("Ops Deck");
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.crypto()).isEqualTo(DashboardMockFixtures.getSummaryCrypto());
        assertThat(response.crypto().values()).containsOnlyKeys("bitcoin", "ethereum");
        assertThat(response.crypto().values().get("bitcoin").change24hPct())
                .isEqualByComparingTo("2.5");
        assertThat(response.equities()).isEqualTo(DashboardMockFixtures.getSummaryEquities());
        assertThat(response.equities().values()).containsOnlyKeys("AAPL", "MSFT", "SPY");
        assertThat(service.getSummary().fx()).isEqualTo(response.fx());
        verify(dolar).fetchDolar();
        org.mockito.Mockito.verifyNoInteractions(fx, news);
        verify(crypto).fetchPrices();
        verify(equities).fetchQuotes();
        org.mockito.Mockito.verifyNoMoreInteractions(crypto, equities);
    }

    @Test
    @DisplayName("Acumula degraded sin duplicados y conserva las otras secciones")
    void aislaFallos() {
        when(dolar.fetchDolar()).thenThrow(new IllegalStateException("offline"));

        var response = service.getSummary();

        assertThat(response.degraded()).containsExactly("fx");
        assertThat(response.fx().quotes()).isEmpty();
        assertThat(response.crypto().values()).isNotEmpty();
        assertThat(response.equities().values()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"fx", "crypto", "equities"})
    @DisplayName("Aísla cada proveedor caído sin perder las otras secciones")
    void aislaCadaProveedor(String section) {
        switch (section) {
            case "fx" -> when(dolar.fetchDolar()).thenThrow(new IllegalStateException("timeout"));
            case "crypto" -> when(crypto.fetchPrices()).thenThrow(new IllegalStateException("timeout"));
            case "equities" -> when(equities.fetchQuotes()).thenThrow(new IllegalStateException("timeout"));
            default -> throw new IllegalArgumentException(section);
        }
        var response = service.getSummary();
        assertThat(response.degraded()).containsExactly(section);
        assertThat(response.fx().quotes().isEmpty()).isEqualTo("fx".equals(section));
        assertThat(response.crypto().values().isEmpty()).isEqualTo("crypto".equals(section));
        assertThat(response.equities().values().isEmpty()).isEqualTo("equities".equals(section));
    }

    @Test
    @DisplayName("Conserva datos stale y marca degraded cuando falla la renovación")
    void marcaDegradedConDatosStale() {
        service = new DashboardService(dolar, crypto, equities, new DashboardCacheService(0), executor);
        service.getSummary();
        when(dolar.fetchDolar()).thenThrow(new IllegalStateException("offline"));

        var response = service.getSummary();

        assertThat(response.fx()).isEqualTo(DashboardMockFixtures.getSummaryFx());
        assertThat(response.degraded()).containsExactly("fx");
    }

    @Test
    @DisplayName("Inicia las tres consultas antes de que termine ninguna")
    void consultaEnParalelo() {
        CountDownLatch started = new CountDownLatch(3);
        when(dolar.fetchDolar()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getDolar()));
        when(crypto.fetchPrices()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getCrypto()));
        when(equities.fetchQuotes()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getEquities()));

        assertThat(service.getSummary().degraded()).isEmpty();
        assertThat(started.getCount()).isZero();
    }

    private <T> T awaitAll(CountDownLatch started, T values) throws InterruptedException {
        started.countDown();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        return values;
    }
}

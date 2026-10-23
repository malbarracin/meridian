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
        service = new DashboardService(dolar, fx, crypto, equities, news, new DashboardCacheService(60), executor);
        when(dolar.fetchDolar()).thenReturn(DashboardMockFixtures.getDolar());
        when(fx.fetchFx()).thenReturn(DashboardMockFixtures.getFx());
        when(crypto.fetchPrices()).thenReturn(DashboardMockFixtures.getCrypto());
        when(crypto.fetchHistory("bitcoin")).thenReturn(DashboardMockFixtures.getCryptoHistory("bitcoin"));
        when(crypto.fetchHistory("ethereum")).thenReturn(DashboardMockFixtures.getCryptoHistory("ethereum"));
        when(equities.fetchQuotes()).thenReturn(DashboardMockFixtures.getEquities());
        when(equities.fetchCandles()).thenReturn(DashboardMockFixtures.getEquityCandles());
        when(news.fetchNews()).thenReturn(DashboardMockFixtures.getNews());
    }

    @AfterEach
    void cierraExecutor() {
        executor.close();
    }

    @Test
    @DisplayName("Agrega todos los adapters y reutiliza los datos en la segunda consulta")
    void agregaYReutilizaCache() {
        var response = service.getDashboard();
        assertThat(response.degraded()).isEmpty();
        assertThat(response.dolar()).isEqualTo(DashboardMockFixtures.getDolar());
        assertThat(response.fx()).isEqualTo(DashboardMockFixtures.getFx());
        assertThat(response.crypto()).isEqualTo(DashboardMockFixtures.getCrypto());
        assertThat(response.cryptoHistory()).hasSize(16);
        assertThat(response.equities()).isEqualTo(DashboardMockFixtures.getEquities());
        assertThat(response.equityCandles()).isEqualTo(DashboardMockFixtures.getEquityCandles());
        assertThat(response.news()).isEqualTo(DashboardMockFixtures.getNews());
        assertThat(service.getDashboard()).isEqualTo(response);
        verify(dolar).fetchDolar();
        verify(fx).fetchFx();
        verify(crypto).fetchPrices();
        verify(crypto).fetchHistory("bitcoin");
        verify(crypto).fetchHistory("ethereum");
        verify(equities).fetchQuotes();
        verify(equities).fetchCandles();
        verify(news).fetchNews();
    }

    @Test
    @DisplayName("Acumula degraded sin duplicados y conserva las otras secciones")
    void aislaFallos() {
        when(dolar.fetchDolar()).thenThrow(new IllegalStateException("offline"));
        when(crypto.fetchHistory("bitcoin")).thenThrow(new IllegalStateException("offline"));
        when(crypto.fetchHistory("ethereum")).thenThrow(new IllegalStateException("offline"));
        when(news.fetchNews()).thenThrow(new IllegalStateException("offline"));

        var response = service.getDashboard();

        assertThat(response.degraded()).containsExactly("dolar", "cryptoHistory", "news");
        assertThat(response.dolar()).isEmpty();
        assertThat(response.cryptoHistory()).isEmpty();
        assertThat(response.news()).isEmpty();
        assertThat(response.crypto()).isNotEmpty();
        assertThat(response.equities()).isNotEmpty();
    }

    @Test
    @DisplayName("Conserva datos stale y marca degraded cuando falla la renovación")
    void marcaDegradedConDatosStale() {
        service = new DashboardService(dolar, fx, crypto, equities, news, new DashboardCacheService(0), executor);
        service.getDashboard();
        when(dolar.fetchDolar()).thenThrow(new IllegalStateException("offline"));

        var response = service.getDashboard();

        assertThat(response.dolar()).isEqualTo(DashboardMockFixtures.getDolar());
        assertThat(response.degraded()).containsExactly("dolar");
        assertThat(response.news()).isEqualTo(DashboardMockFixtures.getNews());
    }

    @Test
    @DisplayName("Inicia las ocho consultas antes de que termine ninguna")
    void consultaEnParalelo() {
        CountDownLatch started = new CountDownLatch(8);
        when(dolar.fetchDolar()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getDolar()));
        when(fx.fetchFx()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getFx()));
        when(crypto.fetchPrices()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getCrypto()));
        when(crypto.fetchHistory("bitcoin")).thenAnswer(call ->
                awaitAll(started, DashboardMockFixtures.getCryptoHistory("bitcoin")));
        when(crypto.fetchHistory("ethereum")).thenAnswer(call ->
                awaitAll(started, DashboardMockFixtures.getCryptoHistory("ethereum")));
        when(equities.fetchQuotes()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getEquities()));
        when(equities.fetchCandles()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getEquityCandles()));
        when(news.fetchNews()).thenAnswer(call -> awaitAll(started, DashboardMockFixtures.getNews()));

        assertThat(service.getDashboard().degraded()).isEmpty();
        assertThat(started.getCount()).isZero();
    }

    private <T> T awaitAll(CountDownLatch started, T values) throws InterruptedException {
        started.countDown();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        return values;
    }
}

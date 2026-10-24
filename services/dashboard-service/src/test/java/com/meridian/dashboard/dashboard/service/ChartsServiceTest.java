package com.meridian.dashboard.dashboard.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.meridian.dashboard.dashboard.adapter.CoinGeckoAdapter;
import com.meridian.dashboard.dashboard.adapter.FinnhubAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.dto.ChartPoint;
import com.meridian.dashboard.dashboard.dto.ChartsResponse;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;
import com.meridian.dashboard.exception.UnsupportedParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Series históricas del dashboard")
class ChartsServiceTest {

    private final CoinGeckoAdapter cryptoAdapter = mock(CoinGeckoAdapter.class);
    private final FinnhubAdapter equityAdapter = mock(FinnhubAdapter.class);
    private final ChartsService service = new ChartsService(cryptoAdapter, equityAdapter,
            new DashboardCacheService(60));

    @ParameterizedTest
    @ValueSource(strings = {"BTC", "ETH"})
    @DisplayName("Criptomonedas usan CoinGecko, normalizan asset y comparten caché")
    void obtieneCrypto(String asset) {
        String symbol = SupportedAsset.valueOf(asset).getSymbol();
        when(cryptoAdapter.fetchHistory(symbol)).thenReturn(DashboardMockFixtures.getCryptoHistory(symbol));

        ChartsResponse response = service.getCharts(asset.toLowerCase(java.util.Locale.ROOT), null);

        assertThat(response.asset()).isEqualTo(asset);
        assertThat(response.range()).isEqualTo("7d");
        assertThat(response.degraded()).isFalse();
        assertThat(response.points()).isEqualTo(DashboardMockFixtures.getChartPoints(asset));
        assertThat(response.points().getLast().t()).isEqualTo(response.points().getFirst().t().plusSeconds(604800));
        assertThat(service.getCharts(asset, "7d")).isEqualTo(response);
        verify(cryptoAdapter, times(1)).fetchHistory(symbol);
        verifyNoInteractions(equityAdapter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "MSFT", "SPY"})
    @DisplayName("Bolsa usa velas Finnhub y convierte segundos Unix y precio de cierre")
    void obtieneEquity(String asset) {
        when(equityAdapter.fetchCandles(asset)).thenReturn(DashboardMockFixtures.getEquityCandles().stream()
                .filter(series -> series.ticker().equals(asset)).findFirst().orElseThrow());

        ChartsResponse response = service.getCharts(asset, "7d");

        assertThat(response.points()).isEqualTo(DashboardMockFixtures.getChartPoints(asset));
        assertThat(response.degraded()).isFalse();
        verify(equityAdapter).fetchCandles(asset);
        verifyNoInteractions(cryptoAdapter);
    }

    @Test
    @DisplayName("Rechaza activo desconocido, ausente y rangos distintos de 7d antes de llamar proveedores")
    void rechazaParametros() {
        assertThatThrownBy(() -> service.getCharts("DOGE", "7d"))
                .isInstanceOf(UnsupportedParameterException.class);
        assertThatThrownBy(() -> service.getCharts(null, "7d"))
                .isInstanceOf(UnsupportedParameterException.class);
        assertThatThrownBy(() -> service.getCharts("BTC", "30d"))
                .isInstanceOf(UnsupportedParameterException.class);
        assertThatThrownBy(() -> service.getCharts("BTC", ""))
                .isInstanceOf(UnsupportedParameterException.class);
        verifyNoInteractions(cryptoAdapter, equityAdapter);
    }

    @Test
    @DisplayName("Fallo sin caché devuelve puntos vacíos y degraded")
    void degradaSinCache() {
        when(cryptoAdapter.fetchHistory("bitcoin")).thenThrow(new IllegalStateException("Proveedor caído"));

        ChartsResponse response = service.getCharts("BTC", "7d");

        assertThat(response.degraded()).isTrue();
        assertThat(response.points()).isEmpty();
    }

    @Test
    @DisplayName("Fallo de Finnhub también degrada sin inventar valores")
    void degradaEquity() {
        when(equityAdapter.fetchCandles("AAPL")).thenThrow(new IllegalStateException("Proveedor caído"));

        ChartsResponse response = service.getCharts("AAPL", "7d");

        assertThat(response.degraded()).isTrue();
        assertThat(response.points()).isEmpty();
    }

    @Test
    @DisplayName("Fallo de renovación conserva la última serie stale")
    void conservaStale() {
        ChartsService staleService = new ChartsService(cryptoAdapter, equityAdapter, new DashboardCacheService(0));
        when(cryptoAdapter.fetchHistory("bitcoin")).thenReturn(DashboardMockFixtures.getCryptoHistory())
                .thenThrow(new IllegalStateException("Timeout"));
        ChartsResponse fresh = staleService.getCharts("BTC", "7d");

        ChartsResponse stale = staleService.getCharts("BTC", "7d");

        assertThat(stale.degraded()).isTrue();
        assertThat(stale.points()).isEqualTo(fresh.points());
    }

    @Test
    @DisplayName("Respuesta copia la colección de puntos para conservar su inmutabilidad")
    void copiaPuntos() {
        List<ChartPoint> points = new ArrayList<>();
        points.add(new ChartPoint(Instant.EPOCH, BigDecimal.ONE));
        ChartsResponse response = new ChartsResponse("BTC", "7d", points, false);

        points.clear();

        assertThat(response.points()).hasSize(1);
        assertThatThrownBy(() -> response.points().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}

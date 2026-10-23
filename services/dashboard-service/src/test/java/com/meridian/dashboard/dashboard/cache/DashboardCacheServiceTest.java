package com.meridian.dashboard.dashboard.cache;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@DisplayName("Cache del dashboard")
class DashboardCacheServiceTest {

    @Test
    @DisplayName("Sirve cache dentro del TTL sin repetir la consulta y renueva al expirar")
    void sirveCacheYRenueva() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        DashboardCacheService cache = new DashboardCacheService(60, clock);
        @SuppressWarnings("unchecked")
        Supplier<List<String>> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(List.of("primero"), List.of("nuevo"));

        assertThat(cache.getOrFetch("dolar", supplier).values()).containsExactly("primero");
        when(clock.instant()).thenReturn(start.plusSeconds(59));
        assertThat(cache.getOrFetch("dolar", supplier).stale()).isFalse();
        verify(supplier).get();
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        assertThat(cache.getOrFetch("dolar", supplier).values()).containsExactly("nuevo");
        verify(supplier, times(2)).get();
    }

    @Test
    @DisplayName("Sirve stale tras fallos sin extender el TTL y se recupera al volver el proveedor")
    void sirveStaleYRecupera() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        DashboardCacheService cache = new DashboardCacheService(60, clock);
        cache.getOrFetch("news", () -> List.of("anterior"));
        when(clock.instant()).thenReturn(start.plusSeconds(60));

        var stale = cache.getOrFetch("news", () -> { throw new IllegalStateException("offline"); });
        assertThat(stale.values()).containsExactly("anterior");
        assertThat(stale.stale()).isTrue();
        assertThat(cache.getOrFetch("news", () -> List.of("nuevo")).values()).containsExactly("nuevo");
    }

    @Test
    @DisplayName("Propaga errores sin cache y respeta el mínimo de 60 segundos para cripto")
    void propagaSinCacheYRespetaMinimoCripto() {
        DashboardCacheService cache = new DashboardCacheService(0, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        assertThatThrownBy(() -> cache.getOrFetch("fx", () -> { throw new IllegalStateException("offline"); }))
                .isInstanceOf(IllegalStateException.class);
        cache.getOrFetch("crypto", () -> List.of("BTC"));
        assertThat(cache.getOrFetch("crypto", () -> { throw new AssertionError("No debe consultar"); }).values())
                .containsExactly("BTC");
    }
}

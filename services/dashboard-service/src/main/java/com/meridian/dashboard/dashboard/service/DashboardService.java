package com.meridian.dashboard.dashboard.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.meridian.dashboard.dashboard.adapter.CoinGeckoAdapter;
import com.meridian.dashboard.dashboard.adapter.DolarApiAdapter;
import com.meridian.dashboard.dashboard.adapter.FinnhubAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService.CacheResult;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.dto.CryptoQuote;
import com.meridian.dashboard.dashboard.dto.CryptoSection;
import com.meridian.dashboard.dashboard.dto.EquitiesSection;
import com.meridian.dashboard.dashboard.dto.EquityQuote;
import com.meridian.dashboard.dashboard.dto.FxSection;
import com.meridian.dashboard.dashboard.dto.RateQuote;
import com.meridian.dashboard.dashboard.dto.SummaryResponse;

import lombok.RequiredArgsConstructor;

/**
 * Orquesta la agregación del dashboard: consulta cada proveedor en paralelo, aplica caché
 * y aísla los fallos por sección para no cortar la respuesta HTTP.
 *
 * <p>Un fallo de un adaptador no propaga excepción al controller: la sección queda en
 * {@code degraded} y se sirve la última respuesta buena de la caché o un mapa vacío.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DolarApiAdapter dolarAdapter;
    private final CoinGeckoAdapter cryptoAdapter;
    private final FinnhubAdapter equityAdapter;
    private final DashboardCacheService cache;
    private final ExecutorService dashboardExecutor;

    /**
     * Construye el resumen de dólar ARS, criptomonedas y bolsa.
     *
     * @return datos disponibles y nombres públicos de las secciones sin datos frescos
     */
    public SummaryResponse getSummary() {
        var dolar = fetch("dolar", dolarAdapter::fetchDolar);
        var crypto = fetch("crypto", cryptoAdapter::fetchPrices);
        var equities = fetch("equities", equityAdapter::fetchQuotes);
        CompletableFuture.allOf(dolar, crypto, equities).join();

        List<String> degraded = Stream.of(
                section("fx", dolar), section("crypto", crypto), section("equities", equities))
                .flatMap(List::stream).distinct().toList();
        return new SummaryResponse("Meridian", Instant.now(),
                new FxSection(dolar.join().values().stream()
                        .filter(quote -> List.of("oficial", "blue").contains(quote.tipo()))
                        .collect(Collectors.toMap(quote -> "usd_" + quote.tipo(),
                                quote -> new RateQuote(quote.compra(), quote.venta())))),
                new CryptoSection(crypto.join().values().stream()
                        .filter(quote -> List.of("bitcoin", "ethereum").contains(quote.id()))
                        .collect(Collectors.toMap(quote -> quote.id(),
                                quote -> new CryptoQuote(quote.priceUsd(), quote.change24h())))),
                new EquitiesSection(equities.join().values().stream()
                        .filter(quote -> List.of("AAPL", "MSFT", "SPY").contains(quote.ticker()))
                        .collect(Collectors.toMap(quote -> quote.ticker(),
                                quote -> new EquityQuote(quote.price(), quote.currency())))), degraded);
    }

    private <T> CompletableFuture<CacheResult<T>> fetch(String key, Supplier<List<T>> supplier) {
        return CompletableFuture.supplyAsync(() -> cache.getOrFetch(key, supplier), dashboardExecutor)
                .exceptionally(failure -> new CacheResult<>(List.of(), true));
    }

    private List<String> section(String name, CompletableFuture<? extends CacheResult<?>> future) {
        return future.join().stale() ? List.of(name) : List.of();
    }
}

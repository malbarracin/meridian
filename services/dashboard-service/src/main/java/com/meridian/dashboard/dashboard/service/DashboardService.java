package com.meridian.dashboard.dashboard.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.meridian.dashboard.dashboard.adapter.CoinGeckoAdapter;
import com.meridian.dashboard.dashboard.adapter.DolarApiAdapter;
import com.meridian.dashboard.dashboard.adapter.FinnhubAdapter;
import com.meridian.dashboard.dashboard.adapter.FrankfurterAdapter;
import com.meridian.dashboard.dashboard.adapter.NewsAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService.CacheResult;
import com.meridian.dashboard.dashboard.dto.DashboardResponse;

import lombok.RequiredArgsConstructor;

/** Agrega las secciones en paralelo y aísla los fallos de cada proveedor. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DolarApiAdapter dolarAdapter;
    private final FrankfurterAdapter fxAdapter;
    private final CoinGeckoAdapter cryptoAdapter;
    private final FinnhubAdapter equityAdapter;
    private final NewsAdapter newsAdapter;
    private final DashboardCacheService cache;
    private final ExecutorService dashboardExecutor;

    /** @return todas las secciones disponibles y los nombres de las que no tienen datos frescos. */
    public DashboardResponse getDashboard() {
        var dolar = fetch("dolar", dolarAdapter::fetchDolar);
        var fx = fetch("fx", fxAdapter::fetchFx);
        var crypto = fetch("crypto", cryptoAdapter::fetchPrices);
        var bitcoin = fetch("cryptoHistory:bitcoin", () -> cryptoAdapter.fetchHistory("bitcoin"));
        var ethereum = fetch("cryptoHistory:ethereum", () -> cryptoAdapter.fetchHistory("ethereum"));
        var equities = fetch("equities", equityAdapter::fetchQuotes);
        var candles = fetch("equityCandles", equityAdapter::fetchCandles);
        var news = fetch("news", newsAdapter::fetchNews);
        CompletableFuture.allOf(dolar, fx, crypto, bitcoin, ethereum, equities, candles, news).join();

        List<String> degraded = Stream.of(
                section("dolar", dolar), section("fx", fx), section("crypto", crypto),
                section("cryptoHistory", bitcoin), section("cryptoHistory", ethereum),
                section("equities", equities), section("equityCandles", candles), section("news", news))
                .flatMap(List::stream).distinct().toList();
        return new DashboardResponse(dolar.join().values(), fx.join().values(), crypto.join().values(),
                Stream.concat(bitcoin.join().values().stream(), ethereum.join().values().stream()).toList(),
                equities.join().values(), candles.join().values(), news.join().values(), degraded);
    }

    private <T> CompletableFuture<CacheResult<T>> fetch(String key, Supplier<List<T>> supplier) {
        return CompletableFuture.supplyAsync(() -> cache.getOrFetch(key, supplier), dashboardExecutor)
                .exceptionally(failure -> new CacheResult<>(List.of(), true));
    }

    private List<String> section(String name, CompletableFuture<? extends CacheResult<?>> future) {
        return future.join().stale() ? List.of(name) : List.of();
    }
}

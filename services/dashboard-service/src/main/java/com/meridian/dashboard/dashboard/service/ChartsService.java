package com.meridian.dashboard.dashboard.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.meridian.dashboard.dashboard.adapter.CoinGeckoAdapter;
import com.meridian.dashboard.dashboard.adapter.FinnhubAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService.CacheResult;
import com.meridian.dashboard.dashboard.dto.ChartPoint;
import com.meridian.dashboard.dashboard.dto.ChartsResponse;
import com.meridian.dashboard.exception.UnsupportedParameterException;

import lombok.RequiredArgsConstructor;

/** Resuelve y almacena series históricas por activo, aislando los fallos del proveedor. */
@Service
@RequiredArgsConstructor
public class ChartsService {

    private static final String SUPPORTED_RANGE = "7d";

    private final CoinGeckoAdapter cryptoAdapter;
    private final FinnhubAdapter equityAdapter;
    private final DashboardCacheService cache;

    /**
     * @param asset símbolo público obligatorio, sin distinción de mayúsculas
     * @param range ventana de siete días; ausente utiliza 7d
     * @return serie fresca, stale o vacía con su indicador de degradación
     */
    public ChartsResponse getCharts(String asset, String range) {
        SupportedAsset supportedAsset = resolveAsset(asset);
        String resolvedRange = range == null ? SUPPORTED_RANGE : range;
        if (!SUPPORTED_RANGE.equals(resolvedRange)) {
            throw new UnsupportedParameterException("range");
        }
        CacheResult<ChartPoint> series;
        try {
            series = cache.getOrFetch("chart:" + supportedAsset.name() + ":" + resolvedRange,
                    () -> fetchPoints(supportedAsset));
        } catch (RuntimeException failure) {
            series = new CacheResult<>(List.of(), true);
        }
        return new ChartsResponse(supportedAsset.name(), resolvedRange, series.values(), series.stale());
    }

    private SupportedAsset resolveAsset(String asset) {
        if (asset == null) {
            throw new UnsupportedParameterException("asset");
        }
        try {
            return SupportedAsset.valueOf(asset.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new UnsupportedParameterException("asset");
        }
    }

    private List<ChartPoint> fetchPoints(SupportedAsset asset) {
        List<ChartPoint> points = asset.isCrypto()
                ? cryptoAdapter.fetchHistory(asset.getSymbol()).stream()
                        .map(point -> new ChartPoint(point.timestamp(), point.priceUsd())).toList()
                : equityAdapter.fetchCandles(asset.getSymbol()).ohlcPoints().stream()
                        .map(point -> new ChartPoint(Instant.ofEpochSecond(point.t()), point.c())).toList();
        return points.stream().sorted(Comparator.comparing(ChartPoint::t)).toList();
    }
}

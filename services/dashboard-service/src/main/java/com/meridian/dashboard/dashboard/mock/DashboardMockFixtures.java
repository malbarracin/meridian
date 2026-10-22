package com.meridian.dashboard.dashboard.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import com.meridian.dashboard.dashboard.dto.CryptoPricePoint;
import com.meridian.dashboard.dashboard.dto.CryptoResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Datos de demostración fijos: BTC 67000 USD (+2.5%), ETH 3500 USD (-1.2%). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DashboardMockFixtures {

    private static final Instant HISTORY_START = Instant.parse("2026-10-15T00:00:00Z");
    private static final int DAYS = 7;
    private static final long SECONDS_PER_DAY = 86400;

    public static List<CryptoResponse> getCrypto() {
        return List.of(
                new CryptoResponse("bitcoin", "BTC", new BigDecimal("67000"), new BigDecimal("2.5")),
                new CryptoResponse("ethereum", "ETH", new BigDecimal("3500"), new BigDecimal("-1.2")));
    }

    /** Serie diaria fija de BTC, mantenida como fixture por defecto. */
    public static List<CryptoPricePoint> getCryptoHistory() {
        return getCryptoHistory("bitcoin");
    }

    /** Siete puntos diarios estáticos para la moneda solicitada. */
    public static List<CryptoPricePoint> getCryptoHistory(String coinId) {
        BigDecimal price = "bitcoin".equals(coinId) ? new BigDecimal("67000") : new BigDecimal("3500");
        return IntStream.rangeClosed(0, DAYS)
                .mapToObj(day -> new CryptoPricePoint(HISTORY_START.plusSeconds(day * SECONDS_PER_DAY), price))
                .toList();
    }
}

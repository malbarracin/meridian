package com.meridian.dashboard.dashboard.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.dto.CryptoPricePoint;
import com.meridian.dashboard.dashboard.dto.CryptoResponse;
import com.meridian.dashboard.dashboard.mapper.CryptoMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

/** Consulta CoinGecko con caché por recurso y límite total de cinco segundos. */
@Component
public class CoinGeckoAdapter {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long MINIMUM_TTL_SECONDS = 60;
    private final WebClient client;
    private final CryptoMapper mapper;
    private final boolean useMock;
    private final Duration ttl;
    private final Clock clock;
    private final Map<String, CacheEntry<?>> cache = new HashMap<>();

    @Autowired
    public CoinGeckoAdapter(@Qualifier("coinGeckoWebClient") WebClient client, CryptoMapper mapper,
            Environment environment) {
        this(client, mapper, environment, Clock.systemUTC());
    }

    CoinGeckoAdapter(WebClient client, CryptoMapper mapper, Environment environment, Clock clock) {
        this.client = client;
        this.mapper = mapper;
        this.useMock = environment.getProperty("dashboard.use-mock", Boolean.class, false);
        this.ttl = Duration.ofSeconds(Math.max(MINIMUM_TTL_SECONDS,
                environment.getProperty("dashboard.cache.ttl-seconds", Long.class, MINIMUM_TTL_SECONDS)));
        this.clock = clock;
    }

    /** Devuelve BTC y ETH; los fallos identifican la sección crypto. */
    public List<CryptoResponse> fetchPrices() {
        if (useMock) {
            return DashboardMockFixtures.getCrypto();
        }
        return cached("crypto", "crypto", () -> {
            JsonNode quotes = request("/api/v3/simple/price?ids=bitcoin,ethereum"
                    + "&vs_currencies=usd&include_24hr_change=true");
            return List.of(mapper.toPrice("bitcoin", "BTC", quotes.required("bitcoin")),
                    mapper.toPrice("ethereum", "ETH", quotes.required("ethereum")));
        });
    }

    /** Devuelve la serie de siete días; los fallos identifican cryptoHistory. */
    public List<CryptoPricePoint> fetchHistory(String coinId) {
        if (!"bitcoin".equals(coinId) && !"ethereum".equals(coinId)) {
            throw new IllegalArgumentException("Moneda no soportada");
        }
        if (useMock) {
            return DashboardMockFixtures.getCryptoHistory(coinId);
        }
        return cached("cryptoHistory:" + coinId, "cryptoHistory", () -> {
            JsonNode prices = request("/api/v3/coins/" + coinId
                    + "/market_chart?vs_currency=usd&days=7").required("prices");
            if (!prices.isArray() || prices.isEmpty()) {
                throw new IllegalArgumentException("Serie histórica inválida");
            }
            return StreamSupport.stream(prices.spliterator(), false).map(mapper::toPoint).toList();
        });
    }

    private JsonNode request(String path) {
        return client.get().uri(path).retrieve().bodyToMono(JsonNode.class)
                .timeout(TIMEOUT).blockOptional().orElseThrow(
                        () -> new IllegalArgumentException("Respuesta vacía de CoinGecko"));
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> List<T> cached(String key, String section, Supplier<List<T>> fetch) {
        CacheEntry<?> previous = cache.get(key);
        if (previous != null && clock.instant().isBefore(previous.expiresAt())) {
            return (List<T>) previous.values();
        }
        try {
            List<T> values = List.copyOf(fetch.get());
            cache.put(key, new CacheEntry<>(values, clock.instant().plus(ttl)));
            return values;
        } catch (RuntimeException exception) {
            throw new CoinGeckoException(section, exception);
        }
    }

    private record CacheEntry<T>(List<T> values, Instant expiresAt) {
    }
}

package com.meridian.dashboard.dashboard.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Cache por recurso con TTL y recuperación de la última respuesta exitosa. */
@Service
public class DashboardCacheService {

    private static final long CRYPTO_MINIMUM_TTL_SECONDS = 60;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public DashboardCacheService(@Value("${dashboard.cache.ttl-seconds:60}") long ttlSeconds) {
        this(ttlSeconds, Clock.systemUTC());
    }

    DashboardCacheService(long ttlSeconds, Clock clock) {
        this.ttl = Duration.ofSeconds(Math.max(0, ttlSeconds));
        this.clock = clock;
    }

    /** Devuelve datos frescos o stale; sin cache propaga el fallo del proveedor. */
    @SuppressWarnings("unchecked")
    public <T> CacheResult<T> getOrFetch(String key, Supplier<List<T>> supplier) {
        CacheEntry previous = cache.get(key);
        Duration resourceTtl = key.startsWith("crypto")
                ? ttl.compareTo(Duration.ofSeconds(CRYPTO_MINIMUM_TTL_SECONDS)) < 0
                        ? Duration.ofSeconds(CRYPTO_MINIMUM_TTL_SECONDS) : ttl
                : ttl;
        if (previous != null && clock.instant().isBefore(previous.updatedAt().plus(resourceTtl))) {
            return new CacheResult<>((List<T>) previous.values(), false);
        }
        try {
            List<T> values = List.copyOf(supplier.get());
            cache.put(key, new CacheEntry(values, clock.instant()));
            return new CacheResult<>(values, false);
        } catch (RuntimeException failure) {
            if (previous == null) {
                throw failure;
            }
            return new CacheResult<>((List<T>) previous.values(), true);
        }
    }

    private record CacheEntry(List<?> values, Instant updatedAt) {
    }

    /** stale indica que el proveedor falló al intentar renovar la entrada vencida. */
    public record CacheResult<T>(List<T> values, boolean stale) {
    }
}

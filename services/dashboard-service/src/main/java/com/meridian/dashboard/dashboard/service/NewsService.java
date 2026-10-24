package com.meridian.dashboard.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.meridian.dashboard.dashboard.adapter.NewsAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService.CacheResult;
import com.meridian.dashboard.dashboard.dto.NewsArticleResponse;
import com.meridian.dashboard.dashboard.dto.NewsResponse;
import com.meridian.dashboard.exception.UnsupportedParameterException;

import lombok.RequiredArgsConstructor;

/** Resuelve noticias IA/tecnología con caché y recuperación ante fallos de ambas fuentes. */
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final String TOPIC = "ai-tech";
    private static final String CACHE_KEY = "news:ai-tech";
    private static final int MAX_ARTICLES = 8;

    private final NewsAdapter newsAdapter;
    private final DashboardCacheService cache;

    /**
     * @param topic tópico opcional; únicamente ai-tech está soportado
     * @param limit máximo opcional entre uno y ocho, por defecto ocho
     * @return noticias frescas, stale o vacías, indicando el fallo de ambas fuentes
     */
    public NewsResponse getNews(String topic, Integer limit) {
        if (topic != null && !TOPIC.equals(topic)) {
            throw new UnsupportedParameterException("topic");
        }
        int maximum = limit == null ? MAX_ARTICLES : limit;
        if (maximum < 1 || maximum > MAX_ARTICLES) {
            throw new UnsupportedParameterException("limit");
        }
        CacheResult<NewsArticleResponse> cachedNews;
        try {
            cachedNews = cache.getOrFetch(CACHE_KEY, newsAdapter::fetchNews);
        } catch (RuntimeException providerFailure) {
            // El adaptador propaga el fallo únicamente tras agotar ambas fuentes y la caché.
            cachedNews = new CacheResult<>(List.of(), true);
        }
        return new NewsResponse(cachedNews.values().stream().limit(maximum).toList(), cachedNews.stale());
    }
}

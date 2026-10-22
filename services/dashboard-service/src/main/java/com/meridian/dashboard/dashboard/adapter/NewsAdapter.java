package com.meridian.dashboard.dashboard.adapter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.meridian.dashboard.dashboard.dto.NewsArticleResponse;
import com.meridian.dashboard.dashboard.mapper.NewsMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

/** Consulta NewsAPI con fallback automático a HN; propaga fallos finales al agregador. */
@Component
public class NewsAdapter {

    private static final int MAX_ARTICLES = 8;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String NEWS_QUERY = "(\"artificial intelligence\" OR AI OR technology)";

    private final WebClient newsApi;
    private final WebClient hnAlgolia;
    private final NewsMapper mapper;
    private final String apiKey;
    private final boolean useMock;

    public NewsAdapter(@Qualifier("newsApiWebClient") WebClient newsApi,
            @Qualifier("hnAlgoliaWebClient") WebClient hnAlgolia, NewsMapper mapper,
            @Value("${dashboard.providers.news-api.api-key:}") String apiKey,
            @Value("${dashboard.use-mock:false}") boolean useMock) {
        this.newsApi = newsApi;
        this.hnAlgolia = hnAlgolia;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.useMock = useMock;
    }

    /** @return hasta ocho noticias; lanza excepción si no hay proveedor disponible para degradar news. */
    public List<NewsArticleResponse> fetchNews() {
        if (useMock) {
            return DashboardMockFixtures.getNews();
        }
        Mono<List<NewsArticleResponse>> feed = apiKey.isBlank() ? fetchHn()
                : fetchNewsApi().onErrorResume(failure -> fetchHn());
        return feed.blockOptional().orElseThrow(() -> new IllegalStateException("Feed de noticias vacío"));
    }

    private Mono<List<NewsArticleResponse>> fetchNewsApi() {
        return newsApi.get().uri(builder -> builder.path("/v2/everything")
                        .queryParam("q", "{query}").queryParam("language", "en")
                        .queryParam("sortBy", "publishedAt").queryParam("pageSize", MAX_ARTICLES)
                        .queryParam("apiKey", "{key}").build(NEWS_QUERY, apiKey))
                .retrieve().bodyToMono(NewsApiFeed.class).timeout(REQUEST_TIMEOUT)
                .switchIfEmpty(Mono.error(new IllegalStateException("Respuesta NewsAPI vacía")))
                .map(feed -> {
                    if (!"ok".equals(feed.status()) || feed.articles() == null) {
                        throw new IllegalStateException("Respuesta NewsAPI inválida");
                    }
                    return feed.articles().stream().limit(MAX_ARTICLES).map(mapper::fromNewsApi).toList();
                });
    }

    private Mono<List<NewsArticleResponse>> fetchHn() {
        return hnAlgolia.get().uri(builder -> builder.path("/api/v1/search")
                        .queryParam("query", "{query}").queryParam("tags", "story").build("AI technology"))
                .retrieve().bodyToMono(HnNewsFeed.class).timeout(REQUEST_TIMEOUT)
                .switchIfEmpty(Mono.error(new IllegalStateException("Respuesta HN vacía")))
                .map(feed -> {
                    if (feed.hits() == null) {
                        throw new IllegalStateException("Respuesta HN inválida");
                    }
                    return feed.hits().stream().limit(MAX_ARTICLES).map(mapper::fromHn).toList();
                });
    }
}

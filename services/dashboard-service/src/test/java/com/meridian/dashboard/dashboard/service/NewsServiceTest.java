package com.meridian.dashboard.dashboard.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.adapter.NewsAdapter;
import com.meridian.dashboard.dashboard.cache.DashboardCacheService;
import com.meridian.dashboard.dashboard.mapper.NewsMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;
import com.meridian.dashboard.exception.UnsupportedParameterException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Servicio de noticias con proveedores y caché reales")
class NewsServiceTest {

    private MockWebServer newsApi;
    private MockWebServer hn;
    private NewsService service;

    @BeforeEach
    void iniciar() throws Exception {
        newsApi = new MockWebServer();
        hn = new MockWebServer();
        newsApi.start();
        hn.start();
        service = new NewsService(new NewsAdapter(WebClient.create(newsApi.url("/").toString()),
                WebClient.create(hn.url("/").toString()), Mappers.getMapper(NewsMapper.class),
                "test-key", false, 2, 2), new DashboardCacheService(0));
    }

    @AfterEach
    void cerrar() throws Exception {
        newsApi.shutdown();
        hn.shutdown();
    }

    @Test
    @DisplayName("NewsAPI disponible devuelve noticias sin degradar y limita después de cachear")
    void usaNewsApi() {
        newsApi.enqueue(newsSuccess());
        assertThat(service.getNews(null, null).items()).hasSize(2);
        assertThat(hn.getRequestCount()).isZero();
        newsApi.enqueue(newsSuccess());
        var response = service.getNews("ai-tech", 1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.degraded()).isFalse();
    }

    @Test
    @DisplayName("NewsAPI caído con fallback HN disponible no degrada")
    void usaFallbackSinDegradar() {
        newsApi.enqueue(new MockResponse().setResponseCode(503));
        hn.enqueue(json("""
                {"hits":[{"objectID":"42","title":"AI","url":"https://example.com/ai",
                "created_at":"2026-01-01T12:00:00Z"}]}
                """));
        var response = service.getNews("ai-tech", 8);
        assertThat(response.items()).hasSize(1);
        assertThat(response.degraded()).isFalse();
        assertThat(hn.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ambas fuentes caídas devuelven vacío o stale con degraded")
    void degradaSoloSiAmbasFallan() {
        failBoth();
        var empty = service.getNews(null, 8);
        assertThat(empty.items()).isEmpty();
        assertThat(empty.degraded()).isTrue();
        newsApi.enqueue(newsSuccess());
        var fresh = service.getNews(null, 8);
        failBoth();
        var stale = service.getNews(null, 8);
        assertThat(stale.items()).isEqualTo(fresh.items());
        assertThat(stale.degraded()).isTrue();
    }

    @Test
    @DisplayName("Comparte la caché news:ai-tech sin guardar el límite de cada petición")
    void comparteCacheEntreLimites() {
        DashboardCacheService cache = new DashboardCacheService(60);
        cache.getOrFetch("news:ai-tech", DashboardMockFixtures::getNews);
        NewsAdapter adapter = org.mockito.Mockito.mock(NewsAdapter.class);
        NewsService cachedService = new NewsService(adapter, cache);

        assertThat(cachedService.getNews(null, 1).items()).hasSize(1);
        assertThat(cachedService.getNews(null, 8).items()).hasSize(2);
        assertThat(cachedService.getNews(null, 8).degraded()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(adapter);
    }

    @Test
    @DisplayName("Valida tópico y límites antes de consultar proveedores")
    void validaParametros() {
        assertThatThrownBy(() -> service.getNews("sports", 8))
                .isInstanceOf(UnsupportedParameterException.class);
        for (int limit : new int[] {0, -1, 9}) {
            assertThatThrownBy(() -> service.getNews(null, limit))
                    .isInstanceOf(UnsupportedParameterException.class);
        }
        assertThat(newsApi.getRequestCount()).isZero();
        assertThat(hn.getRequestCount()).isZero();
    }

    private void failBoth() {
        newsApi.enqueue(new MockResponse().setResponseCode(503));
        hn.enqueue(new MockResponse().setResponseCode(503));
    }

    private MockResponse newsSuccess() {
        return json("""
                {"status":"ok","articles":[
                {"title":"AI","publishedAt":"2026-01-01T12:00:00Z","url":"https://example.com/1"},
                {"title":"Tech","publishedAt":"2026-01-01T12:00:00Z","url":"https://example.com/2"}]}
                """);
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}

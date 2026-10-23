package com.meridian.dashboard.dashboard.adapter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import com.meridian.dashboard.dashboard.dto.NewsArticleResponse;
import com.meridian.dashboard.dashboard.mapper.NewsMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Noticias NewsAPI y fallback HN")
class NewsAdapterTest {

    private MockWebServer newsApi;
    private MockWebServer hn;

    @BeforeEach
    void iniciarProveedores() throws IOException {
        newsApi = new MockWebServer();
        hn = new MockWebServer();
        newsApi.start();
        hn.start();
    }

    @AfterEach
    void cerrarProveedores() throws IOException {
        newsApi.shutdown();
        hn.shutdown();
    }

    @Test
    @DisplayName("Normaliza ocho artículos y envía los parámetros de NewsAPI")
    void normalizaNewsApiYLimitaOcho() throws InterruptedException {
        String articles = IntStream.range(0, 10).mapToObj(index -> """
                {"title":"Article %d","description":"Summary","source":{"name":"Publisher"},
                 "publishedAt":"2026-01-01T12:00:00Z","url":"https://example.com/%d"}
                """.formatted(index, index)).collect(Collectors.joining(","));
        newsApi.enqueue(json("{\"status\":\"ok\",\"articles\":[" + articles + "]}"));

        var articlesResponse = adapter("test-key", false).fetchNews();

        assertThat(articlesResponse).hasSize(8);
        assertThat(articlesResponse.getFirst()).isEqualTo(new NewsArticleResponse(
                "https://example.com/0", "Article 0", "Summary", "Publisher",
                Instant.parse("2026-01-01T12:00:00Z"), "https://example.com/0", "ai-tech"));
        assertThat(articlesResponse).allSatisfy(article -> assertThat(article.topic()).isEqualTo("ai-tech"));
        assertThatThrownBy(articlesResponse::clear).isInstanceOf(UnsupportedOperationException.class);
        var request = newsApi.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        var url = request.getRequestUrl();
        assertThat(url.encodedPath()).isEqualTo("/v2/everything");
        assertThat(url.queryParameter("q")).isEqualTo("(\"artificial intelligence\" OR AI OR technology)");
        assertThat(url.queryParameter("language")).isEqualTo("en");
        assertThat(url.queryParameter("sortBy")).isEqualTo("publishedAt");
        assertThat(url.queryParameter("pageSize")).isEqualTo("8");
        assertThat(url.queryParameter("apiKey")).isNull();
        assertThat(url.toString()).doesNotContain("test-key");
        assertThat(request.getHeader("X-Api-Key")).isEqualTo("test-key");
        assertThat(hn.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Sin key consulta HN y normaliza sus campos")
    void sinKeyUsaHn() throws InterruptedException {
        hn.enqueue(hnSuccess());

        var articles = adapter("  ", false).fetchNews();

        assertThat(articles).containsExactly(new NewsArticleResponse("42", "AI technology", "", "author",
                Instant.parse("2026-01-01T12:00:00Z"), "https://example.com/story", "ai-tech"));
        assertThat(newsApi.getRequestCount()).isZero();
        var request = hn.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getRequestUrl().encodedPath()).isEqualTo("/api/v1/search");
        assertThat(request.getRequestUrl().queryParameter("query")).isEqualTo("AI technology");
        assertThat(request.getRequestUrl().queryParameter("tags")).isEqualTo("story");
    }

    @Test
    @DisplayName("Ante error HTTP de NewsAPI usa HN")
    void errorHttpUsaHn() {
        newsApi.enqueue(new MockResponse().setResponseCode(503));
        hn.enqueue(hnSuccess());

        assertThat(adapter("test-key", false).fetchNews()).hasSize(1);
        assertThat(newsApi.getRequestCount()).isEqualTo(1);
        assertThat(hn.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ante timeout de NewsAPI usa HN")
    void timeoutUsaHn() {
        newsApi.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        hn.enqueue(hnSuccess());

        assertThat(adapter("test-key", false).fetchNews()).hasSize(1);
        assertThat(hn.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si ambos proveedores fallan propaga la excepción para degradar news")
    void falloAmbosPermiteDegradarNews() {
        newsApi.enqueue(new MockResponse().setResponseCode(500));
        hn.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(() -> adapter("test-key", false).fetchNews())
                .isInstanceOf(WebClientResponseException.ServiceUnavailable.class);
        assertThat(newsApi.getRequestCount()).isEqualTo(1);
        assertThat(hn.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Modo mock devuelve dos fixtures sin HTTP")
    void modoMockNoUsaHttp() {
        assertThat(adapter("test-key", true).fetchNews())
                .hasSizeGreaterThanOrEqualTo(2).isEqualTo(DashboardMockFixtures.getNews());
        assertThat(newsApi.getRequestCount()).isZero();
        assertThat(hn.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Una respuesta NewsAPI vacía dispara fallback")
    void respuestaVaciaUsaFallback() {
        newsApi.enqueue(new MockResponse().setResponseCode(204));
        hn.enqueue(hnSuccess());

        assertThat(adapter("test-key", false).fetchNews()).hasSize(1);
    }

    @Test
    @DisplayName("Una respuesta NewsAPI inválida dispara fallback")
    void respuestaInvalidaUsaFallback() {
        newsApi.enqueue(json("{\"status\":\"error\"}"));
        hn.enqueue(hnSuccess());

        assertThat(adapter("test-key", false).fetchNews()).hasSize(1);
    }

    @Test
    @DisplayName("Rechaza un contrato HN inválido")
    void rechazaHnInvalido() {
        hn.enqueue(json("{}"));

        assertThatThrownBy(() -> adapter("", false).fetchNews()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza un cuerpo HN vacío")
    void rechazaHnVacio() {
        hn.enqueue(new MockResponse().setResponseCode(204));

        assertThatThrownBy(() -> adapter("", false).fetchNews()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("NewsAPI permite descripción y fuente ausentes")
    void normalizaCamposOpcionalesNewsApi() {
        newsApi.enqueue(json("""
                {"status":"ok","articles":[{"title":"AI","publishedAt":"2026-01-01T12:00:00Z",
                  "url":"https://example.com/ai"}]}
                """));

        var article = adapter("test-key", false).fetchNews().getFirst();

        assertThat(article.summary()).isEmpty();
        assertThat(article.source()).isEqualTo("NewsAPI");
    }

    @Test
    @DisplayName("HN conserva el resumen y admite autor ausente")
    void normalizaCamposOpcionalesHn() {
        hn.enqueue(json("""
                {"hits":[{"objectID":"43","title":"AI","story_text":"Summary",
                  "created_at":"2026-01-01T12:00:00Z","url":"https://example.com/ai"}]}
                """));

        var article = adapter("", false).fetchNews().getFirst();

        assertThat(article.summary()).isEqualTo("Summary");
        assertThat(article.source()).isEqualTo("Hacker News");
    }

    private NewsAdapter adapter(String key, boolean useMock) {
        return new NewsAdapter(WebClient.create(newsApi.url("/").toString()),
                WebClient.create(hn.url("/").toString()), Mappers.getMapper(NewsMapper.class), key, useMock, 5, 5);
    }

    private MockResponse hnSuccess() {
        return json("""
                {"hits":[{"objectID":"42","title":"AI technology","url":"https://example.com/story",
                  "author":"author","created_at":"2026-01-01T12:00:00Z"}]}
                """);
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}

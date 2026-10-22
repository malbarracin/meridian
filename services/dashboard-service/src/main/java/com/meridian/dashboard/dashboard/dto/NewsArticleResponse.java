package com.meridian.dashboard.dashboard.dto;

import java.time.Instant;

/** Noticia normalizada; id es la URL en NewsAPI o el objectID en HN; topic siempre es ai-tech. */
public record NewsArticleResponse(String id, String title, String summary, String source,
        Instant publishedAt, String url, String topic) {
}

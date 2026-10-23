package com.meridian.dashboard.dashboard.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Noticia normalizada; id es la URL en NewsAPI o el objectID en HN; topic siempre es ai-tech. */
@Schema(description = "Noticia de IA y tecnología normalizada")
public record NewsArticleResponse(
        @Schema(description = "Identificador de la noticia (URL en NewsAPI u objectID en HN)",
                example = "demo-ai") String id,
        @Schema(description = "Título", example = "AI research demo") String title,
        @Schema(description = "Resumen o descripción", example = "Static AI news fixture") String summary,
        @Schema(description = "Fuente de la noticia", example = "Meridian Demo") String source,
        @Schema(description = "Fecha de publicación en UTC", example = "2026-01-01T12:00:00Z")
        Instant publishedAt,
        @Schema(description = "URL del artículo", example = "https://example.com/demo-ai") String url,
        @Schema(description = "Tema de la noticia; siempre ai-tech", example = "ai-tech") String topic) {
}

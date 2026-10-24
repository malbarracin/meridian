package com.meridian.dashboard.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Noticias normalizadas de IA/tecnología.
 * <p>Protegido por JWT. {@code degraded} indica que ninguna fuente pudo renovar los datos.</p>
 *
 * @param items lista de artículos normalizados
 * @param degraded indica fallo de ambas fuentes sin caché fresca
 */
@Schema(description = "Noticias IA/tecnología protegidas por JWT")
public record NewsResponse(
        @Schema(description = "Artículos normalizados (hasta limit)") List<NewsArticleResponse> items,
        @Schema(description = "true solo si fallan NewsAPI y HN Algolia sin caché fresca",
                example = "false") boolean degraded) {

    public NewsResponse {
        items = List.copyOf(items);
    }
}

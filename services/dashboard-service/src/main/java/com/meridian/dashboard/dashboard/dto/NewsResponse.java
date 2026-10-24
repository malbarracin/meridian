package com.meridian.dashboard.dashboard.dto;

import java.util.List;

/** Noticias normalizadas; degraded indica que ninguna fuente pudo renovar los datos. */
public record NewsResponse(List<NewsArticleResponse> items, boolean degraded) {

    public NewsResponse {
        items = List.copyOf(items);
    }
}

package com.meridian.dashboard.dashboard.adapter;

import java.time.Instant;
import java.util.List;

/** Contrato de lectura de NewsAPI. */
public record NewsApiFeed(String status, List<Article> articles) {

    public record Article(String title, String description, Source source, Instant publishedAt, String url) {
    }

    public record Source(String name) {
    }
}

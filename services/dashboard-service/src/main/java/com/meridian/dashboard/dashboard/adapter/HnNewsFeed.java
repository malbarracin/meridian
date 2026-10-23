package com.meridian.dashboard.dashboard.adapter;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Contrato de lectura de historias HN Algolia. */
public record HnNewsFeed(List<Story> hits) {

    public record Story(String objectID, String title, String url, String author,
            @JsonProperty("created_at") Instant createdAt, @JsonProperty("story_text") String storyText) {
    }
}

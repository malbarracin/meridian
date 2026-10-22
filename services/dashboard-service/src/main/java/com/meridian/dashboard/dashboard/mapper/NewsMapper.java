package com.meridian.dashboard.dashboard.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.meridian.dashboard.dashboard.adapter.HnNewsFeed;
import com.meridian.dashboard.dashboard.adapter.NewsApiFeed;
import com.meridian.dashboard.dashboard.dto.NewsArticleResponse;

/** Normaliza los dos contratos externos al feed del dashboard. */
@Mapper(componentModel = "spring")
public interface NewsMapper {

    @Mapping(target = "id", source = "url")
    @Mapping(target = "summary", source = "description", defaultValue = "")
    @Mapping(target = "source", source = "source.name", defaultValue = "NewsAPI")
    @Mapping(target = "topic", constant = "ai-tech")
    NewsArticleResponse fromNewsApi(NewsApiFeed.Article article);

    @Mapping(target = "id", source = "objectID")
    @Mapping(target = "summary", source = "storyText", defaultValue = "")
    @Mapping(target = "source", source = "author", defaultValue = "Hacker News")
    @Mapping(target = "publishedAt", source = "createdAt")
    @Mapping(target = "topic", constant = "ai-tech")
    NewsArticleResponse fromHn(HnNewsFeed.Story story);
}

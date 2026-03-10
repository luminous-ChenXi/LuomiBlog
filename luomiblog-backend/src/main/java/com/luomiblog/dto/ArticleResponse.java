package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String aiSummary;
    private String knowledgePoints;
    private String content;
    private CategoryInfo category;
    private List<TagInfo> tags;
    private AuthorInfo author;
    private String language;
    private String status;
    private String version;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer wordCount;
    private Integer readingTime;
    private Boolean isTop;
    private Boolean allowComments;
    private Boolean allowSuggestions;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagInfo {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
    }
}

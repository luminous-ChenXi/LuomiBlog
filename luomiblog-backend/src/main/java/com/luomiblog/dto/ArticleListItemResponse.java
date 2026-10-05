package com.luomiblog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表条目 DTO（BUG-040 修复）：
 * 列表接口此前复用 ArticleResponse 连 content LONGTEXT 一起返回，
 * 每页 12 篇可达数 MB，2核4G + 3-6Mbps 带宽下次页劣化 2-5 秒。
 * 本 DTO 不含 content/aiSummary/knowledgePoints 等大字段，详情页单独取全文。
 */
@Data
@Builder
public class ArticleListItemResponse {

    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String language;
    private String status;
    private Boolean isTop;

    private Long categoryId;
    private String categoryName;

    private Long authorId;
    private String authorName;

    private List<TagInfo> tags;

    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer wordCount;
    private Integer readingTime;

    private Boolean allowComments;
    private Boolean allowSuggestions;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class TagInfo {
        private Long id;
        private String name;
        private String slug;
    }
}

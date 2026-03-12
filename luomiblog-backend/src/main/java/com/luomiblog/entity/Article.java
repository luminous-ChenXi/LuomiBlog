package com.luomiblog.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "article")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "ai_summary", length = 1000)
    private String aiSummary;

    @Column(name = "knowledge_points", length = 1000)
    private String knowledgePoints;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "reading_time")
    private Integer readingTime;

    @Column(name = "is_top", nullable = false)
    private Boolean top;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "allow_comments", nullable = false)
    private Boolean allowComments;

    @Column(name = "allow_suggestions", nullable = false)
    private Boolean allowSuggestions;

    @Column(name = "sync_bailian", nullable = false)
    private Boolean syncBailian;

    @Column(name = "bailian_doc_id", length = 100)
    private String bailianDocId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

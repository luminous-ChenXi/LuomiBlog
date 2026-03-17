package com.luomiblog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ArticleFileInfo {
    private String fileName;
    private String slug;
    private String title;
    private String description;
    private String content;
    private String author;
    private LocalDateTime pubDate;
    private List<String> tags;
    private String category;
    private String cover;
    private Long fileSize;
    private LocalDateTime fileModifiedTime;
    private String contentHash;
}

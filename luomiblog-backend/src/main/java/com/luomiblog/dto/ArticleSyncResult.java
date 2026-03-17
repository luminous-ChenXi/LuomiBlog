package com.luomiblog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ArticleSyncResult {
    private boolean success;
    private String message;
    private int totalFiles;
    private int createdCount;
    private int updatedCount;
    private int unchangedCount;
    private int conflictCount;
    private List<ConflictInfo> conflicts;
    private LocalDateTime syncTime;
    
    @Data
    @Builder
    public static class ConflictInfo {
        private String slug;
        private String title;
        private String conflictType;
        private String fileModifiedTime;
        private String dbModifiedTime;
        private String suggestion;
    }
}

package com.luomiblog.service;

import com.luomiblog.dto.UploadArticleRequest;

import java.util.List;
import java.util.Map;

public interface ArticleSyncService {

    void syncArticlesFromFiles();

    List<String> detectConflicts();

    void resolveConflict(Long articleId, String resolution);

    boolean isSyncNeeded();

    SyncStatus getSyncStatus();

    Map<String, Object> getSyncDetails();

    Map<String, Object> uploadArticle(UploadArticleRequest request);

    record SyncStatus(
        int totalFiles,
        int syncedArticles,
        int conflicts,
        long lastSyncTime
    ) {}
}

package com.luomiblog.service;

import java.util.List;

public interface ArticleSyncService {
    
    void syncArticlesFromFiles();
    
    List<String> detectConflicts();
    
    void resolveConflict(Long articleId, String resolution);
    
    boolean isSyncNeeded();
    
    SyncStatus getSyncStatus();
    
    record SyncStatus(
        int totalFiles,
        int syncedArticles,
        int conflicts,
        long lastSyncTime
    ) {}
}

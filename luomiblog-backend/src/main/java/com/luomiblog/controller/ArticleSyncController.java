package com.luomiblog.controller;

import com.luomiblog.dto.ApiResponse;
import com.luomiblog.dto.ArticleSyncResult;
import com.luomiblog.service.ArticleSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/articles/sync")
@RequiredArgsConstructor
public class ArticleSyncController {

    private final ArticleSyncService articleSyncService;

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Void>> triggerSync() {
        new Thread(() -> articleSyncService.syncArticlesFromFiles()).start();
        return ResponseEntity.ok(ApiResponse.success(null, "文章同步已启动"));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<ArticleSyncService.SyncStatus>> getSyncStatus() {
        ArticleSyncService.SyncStatus status = articleSyncService.getSyncStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConflicts() {
        var conflicts = articleSyncService.detectConflicts();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "conflicts", conflicts,
            "count", conflicts.size(),
            "needsSync", articleSyncService.isSyncNeeded()
        )));
    }

    @PostMapping("/resolve/{articleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Void>> resolveConflict(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> request) {
        String resolution = request.get("resolution");
        articleSyncService.resolveConflict(articleId, resolution);
        return ResponseEntity.ok(ApiResponse.success(null, "冲突已解决"));
    }

    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSyncNeeded() {
        boolean needed = articleSyncService.isSyncNeeded();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "syncNeeded", needed,
            "message", needed ? "检测到新文章或更新，建议同步" : "文章已是最新状态"
        )));
    }
}

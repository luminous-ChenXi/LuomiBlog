package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.UploadArticleRequest;
import com.luomiblog.service.ArticleSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleSyncController {

    private final ArticleSyncService articleSyncService;

    @PostMapping("/sync/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> triggerSync() {
        new Thread(() -> articleSyncService.syncArticlesFromFiles()).start();
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "文章同步已启动")));
    }

    @GetMapping("/sync/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<ArticleSyncService.SyncStatus>> getSyncStatus() {
        ArticleSyncService.SyncStatus status = articleSyncService.getSyncStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/sync/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConflicts() {
        var conflicts = articleSyncService.detectConflicts();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "conflicts", conflicts,
            "count", conflicts.size(),
            "needsSync", articleSyncService.isSyncNeeded()
        )));
    }

    @PostMapping("/sync/resolve/{articleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> resolveConflict(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> request) {
        String resolution = request.get("resolution");
        articleSyncService.resolveConflict(articleId, resolution);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "冲突已解决")));
    }

    @GetMapping("/sync/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSyncNeeded() {
        boolean needed = articleSyncService.isSyncNeeded();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "syncNeeded", needed,
            "message", needed ? "检测到新文章或更新，建议同步" : "文章已是最新状态"
        )));
    }

    @GetMapping("/sync/check-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSyncDetails() {
        Map<String, Object> details = articleSyncService.getSyncDetails();
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PostMapping("/sync/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> syncAllFiles() {
        articleSyncService.syncArticlesFromFiles();
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "同步完成")));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadArticle(
            @RequestBody UploadArticleRequest request) {
        Map<String, Object> result = articleSyncService.uploadArticle(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

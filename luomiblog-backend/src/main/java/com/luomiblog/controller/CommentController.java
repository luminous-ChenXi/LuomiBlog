package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.CommentRequest;
import com.luomiblog.dto.CommentResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public ApiResponse<Page<CommentResponse>> getCommentsByArticle(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(commentService.getCommentsByArticle(articleId, pageable));
    }

    @GetMapping("/article/{articleId}/tree")
    public ApiResponse<List<CommentResponse>> getCommentTreeByArticle(@PathVariable Long articleId) {
        return ApiResponse.success(commentService.getCommentTreeByArticle(articleId));
    }

    @GetMapping("/user")
    public ApiResponse<Page<CommentResponse>> getCommentsByUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(commentService.getCommentsByUser(userPrincipal.getId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_comment:create')")
    public ApiResponse<CommentResponse> createComment(
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        return ApiResponse.success(commentService.createComment(request, userId, visitorId, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_comment:delete')")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        commentService.deleteComment(id, userPrincipal != null ? userPrincipal.getId() : null);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_comment:manage')")
    public ApiResponse<Void> approveComment(@PathVariable Long id) {
        commentService.approveComment(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_comment:manage')")
    public ApiResponse<Void> rejectComment(@PathVariable Long id) {
        commentService.rejectComment(id);
        return ApiResponse.success();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

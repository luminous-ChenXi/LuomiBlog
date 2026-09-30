package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
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
    private final ClientIpResolver clientIpResolver;
    private final com.luomiblog.service.RateLimitService rateLimitService;

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
        // 统一走 ClientIpResolver：受 app.security.trust-proxy 控制，
        // 直连部署下忽略可伪造的 X-Forwarded-For，防止伪造来源落库/绕过限流
        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        // 限流：10 次/分钟/身份（登录用户按 userId，访客按 visitorId，兜底 IP）
        rateLimitService.checkComment(commentIdentity(userId, visitorId, ipAddress));

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

    /** 限流身份键：登录用户按 userId；访客按 visitorId+IP 绑定（防轮换 X-Visitor-Id 刷新桶）；兜底 IP */
    private String commentIdentity(Long userId, String visitorId, String ipAddress) {
        if (userId != null) {
            return "u:" + userId;
        }
        if (visitorId != null && !visitorId.isBlank()) {
            // 访客身份绑定 IP：X-Visitor-Id 由客户端自带、可随意轮换，仅凭它会拿到全新限流桶
            return "v:" + visitorId + "|" + ipAddress;
        }
        return "ip:" + ipAddress;
    }
}

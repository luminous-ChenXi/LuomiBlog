package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.LikeRequest;
import com.luomiblog.dto.LikeResponse;
import com.luomiblog.entity.User;
import com.luomiblog.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/article")
    public ApiResponse<LikeResponse> toggleArticleLike(
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        Long userId = user != null ? user.getId() : null;

        return ApiResponse.success(likeService.toggleArticleLike(request, userId, visitorId, ipAddress));
    }

    @PostMapping("/comment")
    public ApiResponse<LikeResponse> toggleCommentLike(
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        Long userId = user != null ? user.getId() : null;

        return ApiResponse.success(likeService.toggleCommentLike(request, userId, visitorId, ipAddress));
    }

    @GetMapping("/article/{articleId}")
    public ApiResponse<LikeResponse> getArticleLikeStatus(
            @PathVariable Long articleId,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.success(likeService.getArticleLikeStatus(articleId, userId, visitorId));
    }

    @GetMapping("/comment/{commentId}")
    public ApiResponse<LikeResponse> getCommentLikeStatus(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.success(likeService.getCommentLikeStatus(commentId, userId, visitorId));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.luomiblog.service;

import com.luomiblog.dto.LikeRequest;
import com.luomiblog.dto.LikeResponse;

public interface LikeService {

    LikeResponse toggleArticleLike(LikeRequest request, Long userId, String visitorId, String ipAddress);

    LikeResponse toggleCommentLike(LikeRequest request, Long userId, String visitorId, String ipAddress);

    LikeResponse getArticleLikeStatus(Long articleId, Long userId, String visitorId);

    LikeResponse getCommentLikeStatus(Long commentId, Long userId, String visitorId);
}

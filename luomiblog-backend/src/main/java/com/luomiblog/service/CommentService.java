package com.luomiblog.service;

import com.luomiblog.dto.CommentRequest;
import com.luomiblog.dto.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    CommentResponse createComment(CommentRequest request, Long userId, String visitorId, String ipAddress, String userAgent);

    void deleteComment(Long id, Long userId);

    CommentResponse getCommentById(Long id);

    Page<CommentResponse> getCommentsByArticle(Long articleId, Pageable pageable);

    List<CommentResponse> getCommentTreeByArticle(Long articleId);

    Page<CommentResponse> getCommentsByUser(Long userId, Pageable pageable);

    void approveComment(Long id);

    void rejectComment(Long id);

    void incrementLikeCount(Long commentId);

    void decrementLikeCount(Long commentId);
}

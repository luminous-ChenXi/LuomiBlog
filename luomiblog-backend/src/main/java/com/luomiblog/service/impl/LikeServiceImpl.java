package com.luomiblog.service.impl;

import com.luomiblog.common.BusinessException;
import com.luomiblog.dto.LikeRequest;
import com.luomiblog.dto.LikeResponse;
import com.luomiblog.entity.ArticleLike;
import com.luomiblog.entity.CommentLike;
import com.luomiblog.repository.*;
import com.luomiblog.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LikeServiceImpl implements LikeService {

    private final ArticleLikeRepository articleLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public LikeResponse toggleArticleLike(LikeRequest request, Long userId, String visitorId, String ipAddress) {
        requireIdentity(userId, visitorId);
        Long articleId = request.getTargetId();
        if (!articleRepository.existsById(articleId)) {
            throw new BusinessException(404, "文章不存在");
        }

        boolean hasLiked = hasArticleLiked(articleId, userId, visitorId);
        if (hasLiked) {
            if (userId != null) {
                articleLikeRepository.deleteByArticleIdAndUserId(articleId, userId);
            } else {
                articleLikeRepository.deleteByArticleIdAndVisitorId(articleId, visitorId);
            }
            // [BUG-017 修复] 数据库端原子递减（下限 0），并发不丢计数、不重写 LONGTEXT
            articleRepository.decrementLikeCount(articleId);
            return articleLikeResponse(articleId, false);
        }

        try {
            articleLikeRepository.save(ArticleLike.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .visitorId(visitorId)
                    .ipAddress(ipAddress)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 并发双击撞唯一键：幂等返回已点赞态（旧实现会 500）
            log.info("并发点赞唯一键冲突，按已点赞处理: articleId={}, userId={}, visitorId={}",
                    articleId, userId, visitorId);
            return articleLikeResponse(articleId, true);
        }
        articleRepository.incrementLikeCount(articleId);
        return articleLikeResponse(articleId, true);
    }

    @Override
    @Transactional
    public LikeResponse toggleCommentLike(LikeRequest request, Long userId, String visitorId, String ipAddress) {
        requireIdentity(userId, visitorId);
        Long commentId = request.getTargetId();
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(404, "评论不存在");
        }

        boolean hasLiked = hasCommentLiked(commentId, userId, visitorId);
        if (hasLiked) {
            if (userId != null) {
                commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            } else {
                commentLikeRepository.deleteByCommentIdAndVisitorId(commentId, visitorId);
            }
            commentRepository.decrementLikeCount(commentId);
            return commentLikeResponse(commentId, false);
        }

        try {
            commentLikeRepository.save(CommentLike.builder()
                    .commentId(commentId)
                    .userId(userId)
                    .visitorId(visitorId)
                    .ipAddress(ipAddress)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("并发点赞唯一键冲突，按已点赞处理: commentId={}, userId={}, visitorId={}",
                    commentId, userId, visitorId);
            return commentLikeResponse(commentId, true);
        }
        commentRepository.incrementLikeCount(commentId);
        return commentLikeResponse(commentId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LikeResponse getArticleLikeStatus(Long articleId, Long userId, String visitorId) {
        if (!articleRepository.existsById(articleId)) {
            throw new BusinessException(404, "文章不存在");
        }
        return articleLikeResponse(articleId, hasArticleLiked(articleId, userId, visitorId));
    }

    @Override
    @Transactional(readOnly = true)
    public LikeResponse getCommentLikeStatus(Long commentId, Long userId, String visitorId) {
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(404, "评论不存在");
        }
        return commentLikeResponse(commentId, hasCommentLiked(commentId, userId, visitorId));
    }

    // ==================== 私有辅助 ====================

    /**
     * [BUG-017 修复] 匿名必须携带 visitorId：
     * 唯一键 (article_id, visitor_id) 对 NULL 不去重，缺失即刷赞漏洞入口。
     */
    private void requireIdentity(Long userId, String visitorId) {
        if (userId == null && (visitorId == null || visitorId.isBlank())) {
            throw new BusinessException(400, "缺少有效的访客标识，无法点赞");
        }
    }

    private boolean hasArticleLiked(Long articleId, Long userId, String visitorId) {
        if (userId != null) {
            return articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
        }
        return visitorId != null && articleLikeRepository.existsByArticleIdAndVisitorId(articleId, visitorId);
    }

    private boolean hasCommentLiked(Long commentId, Long userId, String visitorId) {
        if (userId != null) {
            return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        }
        return visitorId != null && commentLikeRepository.existsByCommentIdAndVisitorId(commentId, visitorId);
    }

    private LikeResponse articleLikeResponse(Long articleId, boolean hasLiked) {
        return LikeResponse.builder()
                .targetId(articleId)
                .type("article")
                .hasLiked(hasLiked)
                // 轻量查询取值，不加载含 LONGTEXT 的整行
                .likeCount(articleRepository.getLikeCountById(articleId).orElse(0))
                .build();
    }

    private LikeResponse commentLikeResponse(Long commentId, boolean hasLiked) {
        return LikeResponse.builder()
                .targetId(commentId)
                .type("comment")
                .hasLiked(hasLiked)
                .likeCount(commentRepository.getLikeCountById(commentId).orElse(0))
                .build();
    }
}

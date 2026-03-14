package com.luomiblog.service.impl;

import com.luomiblog.dto.LikeRequest;
import com.luomiblog.dto.LikeResponse;
import com.luomiblog.entity.Article;
import com.luomiblog.entity.ArticleLike;
import com.luomiblog.entity.Comment;
import com.luomiblog.entity.CommentLike;
import com.luomiblog.repository.*;
import com.luomiblog.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Long articleId = request.getTargetId();
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        boolean hasLiked;
        if (userId != null) {
            hasLiked = articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
        } else {
            hasLiked = articleLikeRepository.existsByArticleIdAndVisitorId(articleId, visitorId);
        }

        if (hasLiked) {
            if (userId != null) {
                articleLikeRepository.deleteByArticleIdAndUserId(articleId, userId);
            } else {
                articleLikeRepository.deleteByArticleIdAndVisitorId(articleId, visitorId);
            }
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            articleRepository.save(article);

            return LikeResponse.builder()
                    .targetId(articleId)
                    .type("article")
                    .hasLiked(false)
                    .likeCount(article.getLikeCount())
                    .build();
        } else {
            ArticleLike like = ArticleLike.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .visitorId(visitorId)
                    .ipAddress(ipAddress)
                    .build();
            articleLikeRepository.save(like);

            article.setLikeCount(article.getLikeCount() + 1);
            articleRepository.save(article);

            return LikeResponse.builder()
                    .targetId(articleId)
                    .type("article")
                    .hasLiked(true)
                    .likeCount(article.getLikeCount())
                    .build();
        }
    }

    @Override
    @Transactional
    public LikeResponse toggleCommentLike(LikeRequest request, Long userId, String visitorId, String ipAddress) {
        Long commentId = request.getTargetId();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        boolean hasLiked;
        if (userId != null) {
            hasLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        } else {
            hasLiked = commentLikeRepository.existsByCommentIdAndVisitorId(commentId, visitorId);
        }

        if (hasLiked) {
            if (userId != null) {
                commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            } else {
                commentLikeRepository.deleteByCommentIdAndVisitorId(commentId, visitorId);
            }
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);

            return LikeResponse.builder()
                    .targetId(commentId)
                    .type("comment")
                    .hasLiked(false)
                    .likeCount(comment.getLikeCount())
                    .build();
        } else {
            CommentLike like = CommentLike.builder()
                    .commentId(commentId)
                    .userId(userId)
                    .visitorId(visitorId)
                    .ipAddress(ipAddress)
                    .build();
            commentLikeRepository.save(like);

            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);

            return LikeResponse.builder()
                    .targetId(commentId)
                    .type("comment")
                    .hasLiked(true)
                    .likeCount(comment.getLikeCount())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LikeResponse getArticleLikeStatus(Long articleId, Long userId, String visitorId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        boolean hasLiked = false;
        if (userId != null) {
            hasLiked = articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
        } else if (visitorId != null) {
            hasLiked = articleLikeRepository.existsByArticleIdAndVisitorId(articleId, visitorId);
        }

        return LikeResponse.builder()
                .targetId(articleId)
                .type("article")
                .hasLiked(hasLiked)
                .likeCount(article.getLikeCount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LikeResponse getCommentLikeStatus(Long commentId, Long userId, String visitorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        boolean hasLiked = false;
        if (userId != null) {
            hasLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        } else if (visitorId != null) {
            hasLiked = commentLikeRepository.existsByCommentIdAndVisitorId(commentId, visitorId);
        }

        return LikeResponse.builder()
                .targetId(commentId)
                .type("comment")
                .hasLiked(hasLiked)
                .likeCount(comment.getLikeCount())
                .build();
    }
}

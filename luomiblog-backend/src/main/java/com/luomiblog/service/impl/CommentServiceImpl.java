package com.luomiblog.service.impl;

import com.luomiblog.dto.CommentRequest;
import com.luomiblog.dto.CommentResponse;
import com.luomiblog.entity.Comment;
import com.luomiblog.repository.CommentRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse createComment(CommentRequest request, Long userId, String visitorId, String ipAddress, String userAgent) {
        Comment comment = Comment.builder()
                .articleId(request.getArticleId())
                .parentId(request.getParentId())
                .userId(userId)
                .visitorName(request.getVisitorName())
                .visitorEmail(request.getVisitorEmail())
                .content(request.getContent())
                .status("pending")
                .isTop(false)
                .likeCount(0)
                .replyCount(0)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        commentRepository.save(comment);

        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            parentComment.setReplyCount(parentComment.getReplyCount() + 1);
            commentRepository.save(parentComment);
        }

        return convertToResponse(comment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (userId != null && !userId.equals(comment.getUserId())) {
            throw new RuntimeException("无权删除此评论");
        }

        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        if (comment.getParentId() != null) {
            Comment parentComment = commentRepository.findById(comment.getParentId()).orElse(null);
            if (parentComment != null) {
                parentComment.setReplyCount(Math.max(0, parentComment.getReplyCount() - 1));
                commentRepository.save(parentComment);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        return convertToResponse(comment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByArticle(Long articleId, Pageable pageable) {
        return commentRepository.findByArticleIdAndStatusAndDeletedAtIsNull(articleId, "approved", pageable)
                .map(comment -> convertToResponse(comment, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentTreeByArticle(Long articleId) {
        List<Comment> comments = commentRepository.findByArticleIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(articleId, "approved");

        Map<Long, CommentResponse> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> convertToResponse(c, null)));

        List<CommentResponse> rootComments = new ArrayList<>();

        for (Comment comment : comments) {
            CommentResponse response = commentMap.get(comment.getId());
            if (comment.getParentId() == null) {
                rootComments.add(response);
            } else {
                CommentResponse parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        parent.setReplies(new ArrayList<>());
                    }
                    parent.getReplies().add(response);
                }
            }
        }

        return rootComments;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByUser(Long userId, Pageable pageable) {
        return commentRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(comment -> convertToResponse(comment, userId));
    }

    @Override
    @Transactional
    public void approveComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        comment.setStatus("approved");
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void rejectComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        comment.setStatus("rejected");
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void decrementLikeCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentRepository.save(comment);
    }

    private CommentResponse convertToResponse(Comment comment, Long currentUserId) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        if (comment.getUserId() != null) {
            userRepository.findById(comment.getUserId())
                    .ifPresent(user -> builder.user(
                            CommentResponse.UserInfo.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .nickname(user.getNickname())
                                    .avatarUrl(user.getAvatarUrl())
                                    .build()
                    ));
        } else {
            builder.visitor(CommentResponse.VisitorInfo.builder()
                    .name(comment.getVisitorName())
                    .email(comment.getVisitorEmail())
                    .build());
        }

        return builder.build();
    }
}

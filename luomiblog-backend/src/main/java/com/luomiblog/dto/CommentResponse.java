package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String content;
    private String status;
    private Integer likeCount;
    private Boolean hasLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserInfo user;
    private VisitorInfo visitor;
    private List<CommentResponse> replies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitorInfo {
        private String name;
        private String email;
    }
}

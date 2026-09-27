package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的收藏列表响应：收藏夹清单 + 分页收藏条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyFavoritesResponse {

    /** 当前用户出现过的所有收藏夹名（含默认收藏夹） */
    private List<String> folders;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private List<FavoriteItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteItem {
        private Long favoriteId;
        private Long articleId;
        private String title;
        private String slug;
        private String summary;
        private String categoryName;
        private String authorName;
        private Integer viewCount;
        private Integer likeCount;
        private String folderName;
        private LocalDateTime favoritedAt;
    }
}

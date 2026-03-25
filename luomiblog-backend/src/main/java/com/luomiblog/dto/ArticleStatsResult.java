package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章统计结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsResult {

    /**
     * 是否操作成功
     */
    private boolean success;

    /**
     * 操作类型：like, unlike, favorite, unfavorite, view
     */
    private String action;

    /**
     * 当前点赞数
     */
    private Integer likeCount;

    /**
     * 当前收藏数
     */
    private Integer favoriteCount;

    /**
     * 当前浏览数
     */
    private Integer viewCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean hasLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean hasFavorited;

    /**
     * 提示消息
     */
    private String message;
}

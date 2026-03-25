package com.luomiblog.service;

import com.luomiblog.dto.ArticleStatsResult;

/**
 * 文章统计服务接口
 * 处理浏览量、点赞、收藏等统计功能
 */
public interface ArticleStatsService {

    /**
     * 记录文章浏览（24小时去重）
     *
     * @param articleId  文章ID
     * @param userId     用户ID（登录用户）
     * @param visitorId  访客ID（匿名用户）
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     * @return 是否成功记录（false表示24小时内已浏览过）
     */
    boolean recordView(Long articleId, Long userId, String visitorId, String ipAddress, String userAgent);

    /**
     * 切换点赞状态（点赞/取消点赞）
     *
     * @param articleId 文章ID
     * @param userId    用户ID
     * @param visitorId 访客ID
     * @param ipAddress IP地址
     * @return 点赞结果
     */
    ArticleStatsResult toggleLike(Long articleId, Long userId, String visitorId, String ipAddress);

    /**
     * 切换收藏状态（收藏/取消收藏）
     *
     * @param articleId 文章ID
     * @param userId    用户ID
     * @return 收藏结果
     */
    ArticleStatsResult toggleFavorite(Long articleId, Long userId);

    /**
     * 检查用户是否已点赞
     */
    boolean hasLiked(Long articleId, Long userId, String visitorId);

    /**
     * 检查用户是否已收藏
     */
    boolean hasFavorited(Long articleId, Long userId);

    /**
     * 获取文章统计信息
     */
    ArticleStatsResult getArticleStats(Long articleId, Long userId, String visitorId);
}

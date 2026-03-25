package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仪表盘统计数据 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 文章数量
     */
    private Long totalArticles;

    /**
     * 评论数量
     */
    private Long totalComments;

    /**
     * 今日访问量
     */
    private Long todayViews;

    /**
     * 用户增长数（较昨日）
     */
    private Integer userGrowth;

    /**
     * 文章增长数（较昨日）
     */
    private Integer articleGrowth;

    /**
     * 评论增长数（较昨日）
     */
    private Integer commentGrowth;

    /**
     * 访问增长数（较昨日）
     */
    private Integer viewGrowth;

    /**
     * 最近动态列表
     */
    private List<RecentActivityDTO> recentActivities;

    /**
     * 最近动态 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDTO {
        /**
         * 类型：user, article, comment, system
         */
        private String type;

        /**
         * 动作描述
         */
        private String action;

        /**
         * 详细内容
         */
        private String detail;

        /**
         * 时间描述
         */
        private String time;
    }
}

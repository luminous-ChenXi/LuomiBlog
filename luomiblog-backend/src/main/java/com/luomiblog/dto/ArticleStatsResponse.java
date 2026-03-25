package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsResponse {

    /**
     * 文章总数
     */
    private Long total;

    /**
     * 已发布文章数
     */
    private Long published;

    /**
     * 草稿文章数
     */
    private Long draft;

    /**
     * 已归档文章数
     */
    private Long archived;

    /**
     * 待处理冲突数
     */
    private Long conflicts;
}

package com.luomiblog.service;

import com.luomiblog.dto.DashboardStatsDTO;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {

    /**
     * 获取仪表盘统计数据
     *
     * @return 仪表盘统计数据
     */
    DashboardStatsDTO getDashboardStats();
}

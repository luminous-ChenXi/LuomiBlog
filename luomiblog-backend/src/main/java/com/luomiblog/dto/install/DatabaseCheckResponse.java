package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 数据库连接检查响应
 * 包含连接状态、数据库信息和是否已有数据
 */
@Data
@Builder
public class DatabaseCheckResponse {

    /**
     * 连接是否成功
     */
    private boolean connected;

    /**
     * 连接消息
     */
    private String message;

    /**
     * MySQL 版本
     */
    private String mysqlVersion;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 是否已有数据
     */
    private boolean hasExistingData;

    /**
     * 已有数据提示信息
     */
    private String existingDataMessage;

    /**
     * 检测到的表列表
     */
    private List<String> existingTables;

    /**
     * 是否需要显示重新安装选项
     */
    private boolean needsReinstallOptions;

    /**
     * 详细日志
     */
    private List<String> logs;
}

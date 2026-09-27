package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 数据库测试连接结果（含成功时的版本/字符集与失败时的具体原因分类）
 */
@Data
@Builder
public class DatabaseTestResponse {
    private boolean success;
    private String message;
    /** MySQL 版本号（成功时） */
    private String mysqlVersion;
    /** 服务器字符集（成功时） */
    private String characterSet;
    /** 失败原因分类：CONNECTION_REFUSED / AUTH_FAILED / DATABASE_NOT_EXISTS / VERSION_TOO_LOW / UNKNOWN */
    private String errorType;
    /** 库不存在时为 true，前端据此提供"尝试创建数据库"入口 */
    private boolean databaseMissing;

    @Data
    @Builder
    public static class StepResult {
        private String name;
        private boolean success;
        private String message;
    }
}

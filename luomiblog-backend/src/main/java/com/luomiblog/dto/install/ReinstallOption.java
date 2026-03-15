package com.luomiblog.dto.install;

/**
 * 重新安装选项
 * 类似于 WordPress 的安装方式
 */
public enum ReinstallOption {
    /**
     * 保留现有数据，仅重置配置
     * 适用于：修复安装、更新配置
     */
    KEEP_DATA("keep_data", "保留数据", "保留所有用户、文章、评论等数据，仅重置站点配置"),

    /**
     * 删除所有数据，全新安装
     * 适用于：彻底重新安装、清理测试数据
     */
    FRESH_INSTALL("fresh_install", "全新安装", "清空所有数据，重新开始（不可恢复）"),

    /**
     * 仅更新表结构（保留数据）
     * 适用于：版本升级
     */
    UPDATE_SCHEMA("update_schema", "更新结构", "保留数据，仅更新数据库表结构");

    private final String code;
    private final String name;
    private final String description;

    ReinstallOption(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static ReinstallOption fromCode(String code) {
        for (ReinstallOption option : values()) {
            if (option.code.equals(code)) {
                return option;
            }
        }
        return FRESH_INSTALL; // 默认全新安装
    }
}

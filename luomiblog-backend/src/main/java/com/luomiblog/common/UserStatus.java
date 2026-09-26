package com.luomiblog.common;

/**
 * 用户状态常量
 * 数据库 users.status 字段的取值
 */
public final class UserStatus {

    /** 正常 */
    public static final String ACTIVE = "active";

    /** 禁用 */
    public static final String DISABLED = "disabled";

    private UserStatus() {
    }
}

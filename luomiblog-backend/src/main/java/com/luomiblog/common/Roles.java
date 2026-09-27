package com.luomiblog.common;

/**
 * 角色编码常量（与 roles.code / data.sql 种子数据一致，全小写）。
 *
 * <p>口径约定：代码中一律用 {@code Roles.XXX.equals(roleCode)} 大小写敏感比较；
 * 数据库 code 列为唯一来源（UNIQUE KEY），不允许再出现 equalsIgnoreCase 混用。</p>
 */
public final class Roles {

    /** 会员 */
    public static final String MEMBER = "member";
    /** 博主 */
    public static final String BLOGGER = "blogger";
    /** 管理员 */
    public static final String ADMIN = "admin";

    private Roles() {
    }
}

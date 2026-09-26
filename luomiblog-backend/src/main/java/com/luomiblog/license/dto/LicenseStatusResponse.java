package com.luomiblog.license.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本站授权状态（供前端横幅展示）
 *
 * 状态语义：
 * - disabled：功能未启用（默认），前端不展示任何横幅；
 * - valid：校验通过且缓存未过期；
 * - grace：离线宽限期内（上次校验成功已超 cache-days 或尚未成功过），仅横幅提示；
 * - invalid：超出离线宽限期仍未校验成功，仅横幅提示——绝不锁数据、绝不拒绝服务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusResponse {

    public static final String STATE_DISABLED = "disabled";
    public static final String STATE_VALID = "valid";
    public static final String STATE_GRACE = "grace";
    public static final String STATE_INVALID = "invalid";

    /** 授权状态：disabled / valid / grace / invalid */
    private String state;

    /** 授权校验功能是否启用 */
    private boolean enabled;

    /** 横幅提示文案（state 为 grace/invalid 时非空） */
    private String message;
}

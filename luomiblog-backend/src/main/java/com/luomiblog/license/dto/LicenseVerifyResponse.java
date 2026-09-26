package com.luomiblog.license.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权校验结果（N2 占位，协议只定义到接口层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseVerifyResponse {

    /** 授权是否有效 */
    private boolean valid;

    /** 授权主体（购买者标识） */
    private String licensedTo;

    /** 授权类型（如 personal / commercial） */
    private String licenseType;

    /** 授权到期时间（ISO-8601，如 2027-09-27T00:00:00+08:00；授权按年计） */
    private String expiresAt;

    /** 服务端附言（如宽限提示） */
    private String message;
}

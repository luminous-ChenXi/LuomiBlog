package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 2FA 挑战验证请求：验证码（6 位）或还原码（8 位）二选一
 */
@Data
public class TwoFactorVerifyRequest {

    @NotBlank(message = "挑战令牌不能为空")
    private String challengeToken;

    /** 6 位 TOTP 验证码（与 recoveryCode 二选一） */
    @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字")
    private String code;

    /** 8 位还原码（与 code 二选一，一次性使用） */
    @Pattern(regexp = "\\d{8}", message = "还原码必须是 8 位数字")
    private String recoveryCode;
}

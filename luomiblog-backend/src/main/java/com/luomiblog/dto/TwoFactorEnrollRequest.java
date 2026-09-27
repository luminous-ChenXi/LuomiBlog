package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 2FA 强制绑定确认请求：提交当前 6 位验证码以确认绑定
 */
@Data
public class TwoFactorEnrollRequest {

    @NotBlank(message = "挑战令牌不能为空")
    private String challengeToken;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字")
    private String code;
}

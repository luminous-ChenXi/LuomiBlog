package com.luomiblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册邮箱验证请求：验证码 + 令牌（激活链接会同时携带两者）
 */
@Data
public class EmailVerifyRequest {

    /** 30 分钟有效的验证令牌 */
    @NotBlank(message = "验证令牌不能为空")
    private String token;

    /** 6 位邮箱验证码 */
    @Pattern(regexp = "\\d{6}", message = "验证码必须是 6 位数字")
    private String code;

    /** 收件邮箱（可选，仅用于重发验证码等场景的辅助定位） */
    @Email(message = "邮箱格式不正确")
    private String email;
}

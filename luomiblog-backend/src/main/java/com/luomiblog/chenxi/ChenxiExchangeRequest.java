package com.luomiblog.chenxi;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 辰汐通行证授权码交换请求
 * code：通行证回调带回的授权码；codeVerifier：前端保存的 PKCE 校验器
 */
@Data
public class ChenxiExchangeRequest {

    @NotBlank(message = "授权码不能为空")
    private String code;

    @NotBlank(message = "codeVerifier不能为空")
    private String codeVerifier;
}

package com.luomiblog.dto.install;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaviconConfigRequest {

    /**
     * 图标类型: svg 或 url
     */
    @NotBlank(message = "图标类型不能为空")
    private String type;

    /**
     * SVG 代码或图标 URL
     */
    private String content;
}

package com.luomiblog.dto.install;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SiteConfigRequest {

    @NotBlank(message = "网站名称不能为空")
    private String siteName;

    private String siteDescription;

    @Pattern(regexp = "^(light|dark|auto)$", message = "主题只能是 light、dark 或 auto")
    private String defaultTheme;

    @Pattern(regexp = "^(zh|en|ja)$", message = "语言只能是 zh、en 或 ja")
    private String defaultLanguage;

    private String timezone;
}

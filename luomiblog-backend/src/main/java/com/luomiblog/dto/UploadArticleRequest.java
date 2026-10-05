package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UploadArticleRequest {
    @NotBlank(message = "文件名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5_-]{1,120}\\.md$",
            message = "文件名仅允许字母/数字/中文/下划线/连字符且必须以 .md 结尾")
    private String filename;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private Boolean autoPublish;
    private Boolean skipExisting;
}

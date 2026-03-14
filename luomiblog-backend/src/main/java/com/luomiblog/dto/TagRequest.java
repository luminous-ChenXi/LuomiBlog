package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 120, message = "标签名称不能超过120字")
    private String name;

    private String slug;

    @Size(max = 255, message = "标签描述不能超过255字")
    private String description;

    private String type;
}

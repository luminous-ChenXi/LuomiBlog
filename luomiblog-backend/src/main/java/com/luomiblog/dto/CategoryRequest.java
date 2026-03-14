package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100字")
    private String name;

    private String slug;

    @Size(max = 500, message = "分类描述不能超过500字")
    private String description;

    private Long parentId;

    private Integer sortOrder;
}

package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminStatusChangeRequest {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(active|inactive|banned)$", message = "状态只能是active、inactive或banned")
    private String status;
}

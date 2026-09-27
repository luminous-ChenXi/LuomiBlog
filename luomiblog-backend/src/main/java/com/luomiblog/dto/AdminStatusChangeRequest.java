package com.luomiblog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminStatusChangeRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}

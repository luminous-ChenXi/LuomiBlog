package com.luomiblog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminRoleChangeRequest {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}

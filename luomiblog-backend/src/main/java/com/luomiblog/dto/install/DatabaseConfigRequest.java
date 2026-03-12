package com.luomiblog.dto.install;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DatabaseConfigRequest {

    @NotBlank(message = "数据库主机不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-]+$", message = "数据库主机格式不正确")
    private String host;

    @NotNull(message = "数据库端口不能为空")
    private Integer port;

    @NotBlank(message = "数据库名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "数据库名称只能包含字母、数字和下划线")
    private String database;

    @NotBlank(message = "数据库用户名不能为空")
    private String username;

    @NotBlank(message = "数据库密码不能为空")
    private String password;
}

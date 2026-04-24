package com.luomiblog.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @Size(max = 50, message = "昵称不能超过50字")
    private String nickname;

    @Size(max = 100, message = "邮箱不能超过100字")
    private String email;

    @Size(max = 500, message = "个人简介不能超过500字")
    private String bio;

    @Size(max = 255, message = "个性签名不能超过255字")
    private String signature;

    @Size(max = 120, message = "所在地不能超过120字")
    private String location;

    @Size(max = 255, message = "个人网站不能超过255字")
    private String website;

    private String avatarUrl;
}

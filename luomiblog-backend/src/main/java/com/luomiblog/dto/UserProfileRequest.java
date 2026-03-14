package com.luomiblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {

    @Size(max = 120, message = "昵称不能超过120字")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 180, message = "邮箱不能超过180字")
    private String email;

    @Size(max = 255, message = "个人网站不能超过255字")
    private String website;

    @Size(max = 255, message = "个性签名不能超过255字")
    private String signature;

    @Size(max = 120, message = "所在地不能超过120字")
    private String location;

    @Size(max = 1000, message = "个人简介不能超过1000字")
    private String bio;

    private String avatarUrl;
}

package com.luomiblog.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String nickname;
    private String email;
    private String bio;
    private String signature;
    private String location;
    private String website;
    private String avatarUrl;
}

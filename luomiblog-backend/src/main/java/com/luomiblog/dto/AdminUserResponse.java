package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String bio;
    private String signature;
    private String location;
    private String website;
    private String role;
    private String roleName;
    private Integer roleId;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Long articleCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

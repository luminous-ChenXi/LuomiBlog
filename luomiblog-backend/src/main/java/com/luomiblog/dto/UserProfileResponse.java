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
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private Boolean emailVerified;
    private String avatarUrl;
    private String website;
    private String signature;
    private String location;
    private String bio;
    private String role;
    private String roleName;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}

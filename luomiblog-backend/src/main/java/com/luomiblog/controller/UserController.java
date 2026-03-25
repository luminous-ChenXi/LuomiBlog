package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.PasswordChangeRequest;
import com.luomiblog.dto.UserProfileRequest;
import com.luomiblog.dto.UserProfileResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(userService.getUserProfile(userPrincipal.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserProfileRequest request) {
        return ApiResponse.success(userService.updateUserProfile(userPrincipal.getId(), request));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userPrincipal.getId(), request);
        return ApiResponse.success();
    }

    @PostMapping("/verify-code")
    public ApiResponse<Void> sendVerifyCode(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.sendVerifyCode(userPrincipal.getId());
        return ApiResponse.success();
    }

    @PostMapping("/avatar")
    public ApiResponse<Void> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String avatarUrl) {
        userService.uploadAvatar(userPrincipal.getId(), avatarUrl);
        return ApiResponse.success();
    }
}

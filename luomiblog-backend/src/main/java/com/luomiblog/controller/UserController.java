package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.PasswordChangeRequest;
import com.luomiblog.dto.UserProfileRequest;
import com.luomiblog.dto.UserProfileResponse;
import com.luomiblog.entity.User;
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
    public ApiResponse<UserProfileResponse> getUserProfile(@AuthenticationPrincipal User user) {
        return ApiResponse.success(userService.getUserProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileRequest request) {
        return ApiResponse.success(userService.updateUserProfile(user.getId(), request));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(user.getId(), request);
        return ApiResponse.success();
    }

    @PostMapping("/verify-code")
    public ApiResponse<Void> sendVerifyCode(@AuthenticationPrincipal User user) {
        userService.sendVerifyCode(user.getId());
        return ApiResponse.success();
    }

    @PostMapping("/avatar")
    public ApiResponse<Void> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam String avatarUrl) {
        userService.uploadAvatar(user.getId(), avatarUrl);
        return ApiResponse.success();
    }
}

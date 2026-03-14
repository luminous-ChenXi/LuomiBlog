package com.luomiblog.service;

import com.luomiblog.dto.PasswordChangeRequest;
import com.luomiblog.dto.UserProfileRequest;
import com.luomiblog.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getUserProfile(Long userId);

    UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    void sendVerifyCode(Long userId);

    void uploadAvatar(Long userId, String avatarUrl);
}

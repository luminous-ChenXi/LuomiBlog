package com.luomiblog.service;

import com.luomiblog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> getUsers(Pageable pageable, String search, String role, String status);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request, Long operatorId);

    AdminUserResponse changeRole(Long id, AdminRoleChangeRequest request, Long operatorId);

    AdminUserResponse changeStatus(Long id, AdminStatusChangeRequest request, Long operatorId);

    void deleteUser(Long id, Long operatorId);

    void resetPassword(Long id, AdminResetPasswordRequest request, Long operatorId);

    /**
     * 重置指定用户的 2FA 绑定（用户下次登录将重新进入强制绑定流程）
     *
     * @param operatorUsername 操作者用户名（JWT 认证过滤器以用户名作为 principal）
     */
    void resetTwoFactor(Long id, String operatorUsername);
}

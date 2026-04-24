package com.luomiblog.service;

import com.luomiblog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> getUsers(Pageable pageable, String search, String role, String status);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request);

    AdminUserResponse changeRole(Long id, AdminRoleChangeRequest request);

    AdminUserResponse changeStatus(Long id, AdminStatusChangeRequest request);

    void deleteUser(Long id);

    void resetPassword(Long id, AdminResetPasswordRequest request);
}

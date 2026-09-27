package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.*;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(adminUserService.getUsers(pageable, search, role, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal operator) {
        return ApiResponse.success(adminUserService.updateUser(id, request, operator.getId()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleChangeRequest request,
            @AuthenticationPrincipal UserPrincipal operator) {
        return ApiResponse.success(adminUserService.changeRole(id, request, operator.getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusChangeRequest request,
            @AuthenticationPrincipal UserPrincipal operator) {
        return ApiResponse.success(adminUserService.changeStatus(id, request, operator.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal operator) {
        adminUserService.deleteUser(id, operator.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request,
            @AuthenticationPrincipal UserPrincipal operator) {
        adminUserService.resetPassword(id, request, operator.getId());
        return ApiResponse.success();
    }
}

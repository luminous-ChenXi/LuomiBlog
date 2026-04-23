package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMenuController {

    private final PermissionService permissionService;

    @GetMapping("/menus")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<List<PermissionService.MenuPermissionItem>> getAdminMenus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        @SuppressWarnings("null")
        List<PermissionService.MenuPermissionItem> menus =
                permissionService.getAdminMenuByRoleCode(userPrincipal.getRoleCode());
        return ApiResponse.success(menus);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<Map<String, Object>> getCurrentUserPermissions(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(Map.of(
                "role", userPrincipal.getRoleCode(),
                "permissions", userPrincipal.getPermissions(),
                "isBloggerOrAdmin", userPrincipal.isBloggerOrAdmin(),
                "isAdmin", userPrincipal.isAdmin()
        ));
    }
}

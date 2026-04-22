package com.luomiblog.service;

import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissionCodesByRoleId(@NonNull Integer roleId);

    Set<String> getPermissionCodesByUserId(@NonNull Long userId);

    boolean hasPermission(@NonNull Long userId, @NonNull String permissionCode);

    boolean hasAnyPermission(@NonNull Long userId, @NonNull String... permissionCodes);

    boolean hasRole(@NonNull Long userId, @NonNull String roleCode);

    boolean isBloggerOrAdmin(@NonNull Long userId);

    boolean isAdmin(@NonNull Long userId);

    List<MenuPermissionItem> getAdminMenuByRoleCode(@NonNull String roleCode);

    record MenuPermissionItem(
            Long id,
            String name,
            String code,
            String path,
            String icon,
            Long parentId,
            String permissionCode,
            List<String> visibleRoles,
            Integer sortOrder
    ) {}
}

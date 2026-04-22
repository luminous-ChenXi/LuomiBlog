package com.luomiblog.service.impl;

import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.MemoryCacheService;
import com.luomiblog.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PermissionServiceImpl implements PermissionService {

    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MemoryCacheService memoryCacheService;

    private static final long PERMISSION_CACHE_TTL = 300;

    @Override
    public Set<String> getPermissionCodesByRoleId(@NonNull Integer roleId) {
        String cacheKey = "perm:role:" + roleId;
        Set<String> cached = (Set<String>) memoryCacheService.get(cacheKey, Set.class);
        if (cached != null) {
            return cached;
        }

        try {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT p.code FROM permissions p " +
                    "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                    "WHERE rp.role_id = ?",
                    String.class, roleId);
            Set<String> result = new HashSet<>(codes);
            memoryCacheService.set(cacheKey, result, PERMISSION_CACHE_TTL);
            return result;
        } catch (Exception e) {
            log.error("获取角色权限失败, roleId={}: {}", roleId, e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getPermissionCodesByUserId(@NonNull Long userId) {
        String cacheKey = "perm:user:" + userId;
        Set<String> cached = (Set<String>) memoryCacheService.get(cacheKey, Set.class);
        if (cached != null) {
            return cached;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Collections.emptySet();
        }
        User user = userOpt.get();
        Integer roleId = user.getRoleId();
        if (roleId == null) {
            return Collections.emptySet();
        }

        Set<String> result = getPermissionCodesByRoleId(roleId);
        memoryCacheService.set(cacheKey, result, PERMISSION_CACHE_TTL);
        return result;
    }

    @Override
    public boolean hasPermission(@NonNull Long userId, @NonNull String permissionCode) {
        return getPermissionCodesByUserId(userId).contains(permissionCode);
    }

    @Override
    public boolean hasAnyPermission(@NonNull Long userId, @NonNull String... permissionCodes) {
        Set<String> userPerms = getPermissionCodesByUserId(userId);
        return Arrays.stream(permissionCodes).anyMatch(userPerms::contains);
    }

    @Override
    public boolean hasRole(@NonNull Long userId, @NonNull String roleCode) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        Integer roleId = user.getRoleId();
        if (roleId == null) {
            return false;
        }
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        return roleOpt.isPresent() && roleOpt.get().getCode().equalsIgnoreCase(roleCode);
    }

    @Override
    public boolean isBloggerOrAdmin(@NonNull Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        Integer roleId = user.getRoleId();
        if (roleId == null) {
            return false;
        }
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return false;
        }
        String code = roleOpt.get().getCode().toLowerCase();
        return "blogger".equals(code) || "admin".equals(code);
    }

    @Override
    public boolean isAdmin(@NonNull Long userId) {
        return hasRole(userId, "admin");
    }

    @Override
    public List<MenuPermissionItem> getAdminMenuByRoleCode(@NonNull String roleCode) {
        String cacheKey = "menu:role:" + roleCode;
        List<MenuPermissionItem> cached = (List<MenuPermissionItem>) memoryCacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, name, code, path, icon, parent_id, permission_code, visible_roles, sort_order " +
                    "FROM admin_menu WHERE is_enabled = 1 ORDER BY sort_order ASC");

            List<MenuPermissionItem> allMenus = rows.stream()
                    .map(this::mapToMenuItem)
                    .collect(Collectors.toList());

            List<MenuPermissionItem> filtered = allMenus.stream()
                    .filter(item -> {
                        if (item.visibleRoles() == null || item.visibleRoles().isEmpty()) {
                            return true;
                        }
                        return item.visibleRoles().contains(roleCode);
                    })
                    .collect(Collectors.toList());

            memoryCacheService.set(cacheKey, filtered, PERMISSION_CACHE_TTL);
            return filtered;
        } catch (Exception e) {
            log.error("获取管理菜单失败, roleCode={}: {}", roleCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    private MenuPermissionItem mapToMenuItem(Map<String, Object> row) {
        List<String> visibleRoles = null;
        Object vrObj = row.get("visible_roles");
        if (vrObj instanceof String json) {
            try {
                json = json.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "");
                visibleRoles = Arrays.asList(json.split(","));
            } catch (Exception e) {
                log.warn("解析 visible_roles 失败: {}", vrObj);
            }
        } else if (vrObj instanceof List<?> list) {
            visibleRoles = (List<String>) list;
        }

        return new MenuPermissionItem(
                ((Number) row.get("id")).longValue(),
                (String) row.get("name"),
                (String) row.get("code"),
                (String) row.get("path"),
                (String) row.get("icon"),
                row.get("parent_id") != null ? ((Number) row.get("parent_id")).longValue() : null,
                (String) row.get("permission_code"),
                visibleRoles,
                ((Number) row.get("sort_order")).intValue()
        );
    }
}

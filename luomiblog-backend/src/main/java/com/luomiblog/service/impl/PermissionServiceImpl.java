package com.luomiblog.service.impl;

import com.luomiblog.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<String> getPermissionCodesByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptySet();
        }

        try {
            String sql = "SELECT p.code FROM permissions p " +
                    "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                    "WHERE rp.role_id = ?";

            return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, roleId));
        } catch (Exception e) {
            log.warn("查询角色权限失败, roleId={}: {}", roleId, e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    public List<MenuPermissionItem> getAdminMenuByRoleCode(String roleCode) {
        MenuPermissionItem dashboard = new MenuPermissionItem("dashboard", "仪表盘", "layout-dashboard", "/admin/dashboard", null);

        MenuPermissionItem articleMenu = new MenuPermissionItem("articles", "文章管理", "file-text", "/admin/articles", List.of(
                new MenuPermissionItem("article-list", "文章列表", "list", "/admin/articles", null),
                new MenuPermissionItem("article-create", "写文章", "plus", "/admin/articles/create", null),
                new MenuPermissionItem("categories", "分类管理", "folder", "/admin/categories", null),
                new MenuPermissionItem("tags", "标签管理", "tag", "/admin/tags", null)
        ));

        MenuPermissionItem commentMenu = new MenuPermissionItem("comments", "评论管理", "message-square", "/admin/comments", null);

        MenuPermissionItem userMenu = new MenuPermissionItem("users", "用户管理", "users", "/admin/users", null);

        MenuPermissionItem systemMenu = new MenuPermissionItem("system", "系统设置", "settings", "/admin/system", List.of(
                new MenuPermissionItem("system-general", "基本设置", "wrench", "/admin/system/general", null),
                new MenuPermissionItem("system-roles", "角色权限", "shield", "/admin/system/roles", null)
        ));

        if ("admin".equalsIgnoreCase(roleCode)) {
            return List.of(dashboard, articleMenu, commentMenu, userMenu, systemMenu);
        } else if ("blogger".equalsIgnoreCase(roleCode)) {
            return List.of(dashboard, articleMenu, commentMenu);
        }

        return List.of(dashboard);
    }
}

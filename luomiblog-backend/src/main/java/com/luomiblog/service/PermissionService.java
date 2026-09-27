package com.luomiblog.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissionCodesByRoleId(Long roleId);

    List<MenuPermissionItem> getAdminMenuByRoleCode(String roleCode);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class MenuPermissionItem {
        private String key;
        private String label;
        private String icon;
        private String path;
        private List<MenuPermissionItem> children;
    }
}

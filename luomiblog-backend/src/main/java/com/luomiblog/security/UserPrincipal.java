package com.luomiblog.security;

import com.luomiblog.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
public class UserPrincipal implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String roleCode;
    private final Set<String> permissions;

    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities, String roleCode, Set<String> permissions) {
        this.user = user;
        this.authorities = authorities;
        this.roleCode = roleCode;
        this.permissions = permissions;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"banned".equals(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "active".equals(user.getStatus());
    }

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public Integer getRoleId() {
        return user.getRoleId();
    }

    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    public boolean isBloggerOrAdmin() {
        return "blogger".equalsIgnoreCase(roleCode) || "admin".equalsIgnoreCase(roleCode);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(roleCode);
    }
}

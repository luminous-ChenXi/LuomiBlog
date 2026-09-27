package com.luomiblog.security;

import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findActiveByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findActiveByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + usernameOrEmail)));

        String roleCode = "visitor";
        Long roleId = user.getRoleId();
        if (roleId != null) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                roleCode = role.getCode().toLowerCase();
            }
        }

        Set<String> permissionCodes = roleId != null
                ? permissionService.getPermissionCodesByRoleId(roleId)
                : Collections.emptySet();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
        for (String perm : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
        }

        return new UserPrincipal(user, authorities, roleCode, permissionCodes);
    }
}

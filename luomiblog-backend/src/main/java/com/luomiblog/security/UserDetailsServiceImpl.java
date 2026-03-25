package com.luomiblog.security;

import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findActiveByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findActiveByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + usernameOrEmail)));

        // 根据 roleId 获取角色
        String roleName = "USER";
        Integer roleId = user.getRoleId();
        if (roleId != null) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                roleName = role.getCode().toUpperCase();
            }
        }

        // 使用自定义的 UserPrincipal 包装 User 实体
        return new UserPrincipal(user, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName)));
    }
}

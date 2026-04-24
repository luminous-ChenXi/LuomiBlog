package com.luomiblog.service.impl;

import com.luomiblog.dto.*;
import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.ArticleRepository;
import com.luomiblog.repository.CommentRepository;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable, String search, String role, String status) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), likePattern),
                        cb.like(root.get("nickname"), likePattern),
                        cb.like(root.get("email"), likePattern)
                ));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (role != null && !role.trim().isEmpty()) {
                Role roleEntity = roleRepository.findByCode(role).orElse(null);
                if (roleEntity != null) {
                    predicates.add(cb.equal(root.get("roleId"), roleEntity.getId()));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(this::convertToAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }
        return convertToAdminResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            user.setWebsite(request.getWebsite());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);
        log.info("管理员更新用户信息: userId={}", id);
        return convertToAdminResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse changeRole(Long id, AdminRoleChangeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }

        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        user.setRoleId(newRole.getId());
        userRepository.save(user);
        log.info("管理员变更用户角色: userId={}, newRole={}", id, newRole.getCode());
        return convertToAdminResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse changeStatus(Long id, AdminStatusChangeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }

        user.setStatus(request.getStatus());
        userRepository.save(user);
        log.info("管理员变更用户状态: userId={}, newStatus={}", id, request.getStatus());
        return convertToAdminResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setStatus("inactive");
        userRepository.save(user);
        log.info("管理员删除用户: userId={}", id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("用户已被删除");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("管理员重置用户密码: userId={}", id);
    }

    private AdminUserResponse convertToAdminResponse(User user) {
        AdminUserResponse.AdminUserResponseBuilder builder = AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .signature(user.getSignature())
                .location(user.getLocation())
                .website(user.getWebsite())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getRoleId() != null) {
            roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                builder.role(role.getCode());
                builder.roleName(role.getName());
            });
        }

        try {
            long articleCount = articleRepository.findByAuthorId(user.getId(), Pageable.unpaged()).getTotalElements();
            builder.articleCount(articleCount);
        } catch (Exception e) {
            builder.articleCount(0L);
        }

        try {
            long commentCount = commentRepository.findByUserIdAndDeletedAtIsNull(user.getId(), Pageable.unpaged()).getTotalElements();
            builder.commentCount(commentCount);
        } catch (Exception e) {
            builder.commentCount(0L);
        }

        return builder.build();
    }
}

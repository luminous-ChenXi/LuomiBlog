package com.luomiblog.repository;

import com.luomiblog.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByCode(String code);

    Optional<Role> findByName(String name);
}

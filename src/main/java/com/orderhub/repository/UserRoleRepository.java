package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.orderhub.entity.UserRole;
import com.orderhub.entity.UserRoleId;

import jakarta.transaction.Transactional;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.id.userId = :userId AND ur.role.name = :roleName")
    boolean existsByUserIdAndRoleName(@Param("userId") UUID userId, @Param("roleName") String roleName);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur WHERE ur.id.userId = :userId AND ur.role.name = :roleName")
    void deleteByUserIdAndRoleName(@Param("userId") UUID userId, @Param("roleName") String roleName);

}
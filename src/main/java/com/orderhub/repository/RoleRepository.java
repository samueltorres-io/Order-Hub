package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.orderhub.entity.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}

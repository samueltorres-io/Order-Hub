package com.orderhub.service;

import java.lang.foreign.Linker.Option;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.orderhub.entity.Role;
import com.orderhub.entity.User;
import com.orderhub.entity.UserRole;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.RoleRepository;
import com.orderhub.repository.UserRepository;
import com.orderhub.repository.UserRoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public boolean verifyRole(UUID userId, String roleName) {
        return userRoleRepository.existsByUserIdAndRoleName(userId, roleName);
    }

    @Transactional
    public void associateRole(UUID userId, String roleName) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND));

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT /* Evitar dizer que a role não existe */ , HttpStatus.NOT_FOUND));
        
        if (this.verifyRole(userId, roleName)) {
            throw new AppException(ErrorCode.ASSOCIATION_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
    }

    /* Desassociar Permissão (userId, roleName) */
    public boolean unlinkRole(UUID userId, String roleName) {
        /* Verifica se a associação realmente existe */
        /* remove */
    }

}

package com.orderhub.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import com.orderhub.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRoleRepository userRoleRepository;

    public boolean verifyRole(UUID userId, String roleName) {
        return userRoleRepository.existsByUserIdAndRoleName(userId, roleName);
    }

    /* Associar Permissão (userId, roleName) */
    public boolean associateRole(UUID userId, String roleName) {
        /* User existe */
        /* Role existe */
        /* Já existe associação: this.verifyRole */
        /* Cria associação */
    }

    /* Desassociar Permissão (userId, roleName) */
    pubilc boolean unlinkRole(UUID userId, String roleName) {
        /* Verifica se a associação realmente existe */
        /* remove */
    }

}

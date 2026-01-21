package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.orderhub.entity.Role;
import com.orderhub.entity.User;
import com.orderhub.entity.UserRole;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.RoleRepository;
import com.orderhub.repository.UserRepository;
import com.orderhub.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Nested
    @DisplayName("Tests for verifyRole()")
    class VerifyRoleTests {

        @Test
        @DisplayName("Should return true when association exists")
        void verifyRole_True() {
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN))
                .thenReturn(true);

            boolean result = roleService.verifyRole(USER_ID, ROLE_ADMIN);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when association does not exist")
        void verifyRole_False() {
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN))
                .thenReturn(false);

            boolean result = roleService.verifyRole(USER_ID, ROLE_ADMIN);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for associateRole()")
    class AssociateRoleTests {

        @Test
        @DisplayName("Should associate role successfully")
        void associateRole_Success() {
            User mockUser = new User();
            mockUser.setId(USER_ID);
            
            Role mockRole = new Role();
            mockRole.setName(ROLE_ADMIN);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(roleRepository.findByName(ROLE_ADMIN)).thenReturn(Optional.of(mockRole));
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN)).thenReturn(false);

            roleService.associateRole(USER_ID, ROLE_ADMIN);

            verify(userRoleRepository).save(any(UserRole.class));
        }

        @Test
        @DisplayName("Should throw exception when User not found")
        void associateRole_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.associateRole(USER_ID, ROLE_ADMIN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USR_NOT_FOUND)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

            verify(userRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when Role not found")
        void associateRole_RoleNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
            when(roleRepository.findByName(ROLE_ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.associateRole(USER_ID, ROLE_ADMIN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT) 
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

            verify(userRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when Association already exists")
        void associateRole_AlreadyExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
            when(roleRepository.findByName(ROLE_ADMIN)).thenReturn(Optional.of(new Role()));
            
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN)).thenReturn(true);

            assertThatThrownBy(() -> roleService.associateRole(USER_ID, ROLE_ADMIN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ASSOCIATION_ALREADY_EXISTS)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

            verify(userRoleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for unlinkRole()")
    class UnlinkRoleTests {

        @Test
        @DisplayName("Should unlink role successfully")
        void unlinkRole_Success() {
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN)).thenReturn(true);

            roleService.unlinkRole(USER_ID, ROLE_ADMIN);

            verify(userRoleRepository).deleteByUserIdAndRoleName(USER_ID, ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should throw exception when association does not exist")
        void unlinkRole_NotFound() {
            when(userRoleRepository.existsByUserIdAndRoleName(USER_ID, ROLE_ADMIN)).thenReturn(false);

            assertThatThrownBy(() -> roleService.unlinkRole(USER_ID, ROLE_ADMIN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ASSOCIATION_DOES_NOT_EXISTS)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

            verify(userRoleRepository, never()).deleteByUserIdAndRoleName(any(), any());
        }
    }
}
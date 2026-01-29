package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.entity.User;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final String VALID_USERNAME = "johndoe";
    private static final String VALID_EMAIL = "john@example.com";
    private static final String STRONG_PASSWORD = "StrongPass123!"; 
    private static final String ENCODED_PASSWORD = "encodedHashSequence";

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("Should create user successfully when input is valid")
        void create_Success() {
            Register request = new Register(VALID_USERNAME, VALID_EMAIL, STRONG_PASSWORD);
            
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            // Mock do save retornando o usuário com ID gerado
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            User createdUser = userService.create(request);

            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getUsername()).isEqualTo(VALID_USERNAME);
            assertThat(createdUser.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(createdUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void create_EmailAlreadyExists() {
            Register request = new Register(VALID_USERNAME, VALID_EMAIL, STRONG_PASSWORD);
            
            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USR_ALREADY_EXISTS)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when password is weak")
        void create_WeakPassword() {
            // Esse teste continua relevante pois a validação de regex da senha está explícita no Service
            String weakPass = "WeakPass123"; 
            Register request = new Register(VALID_USERNAME, VALID_EMAIL, weakPass);

            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEAK_PASSWORD)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for login()")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() {
            Login request = new Login(VALID_EMAIL, STRONG_PASSWORD);
            User mockUser = new User();
            mockUser.setEmail(VALID_EMAIL);
            mockUser.setPasswordHash(ENCODED_PASSWORD);

            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(STRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            User loggedUser = userService.login(request);

            assertThat(loggedUser).isEqualTo(mockUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found by email")
        void login_UserNotFound() {
            Login request = new Login("unknown@mail.com", STRONG_PASSWORD);

            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should throw exception when password does not match")
        void login_WrongPassword() {
            Login request = new Login(VALID_EMAIL, "WrongPass123!");
            User mockUser = new User();
            mockUser.setPasswordHash(ENCODED_PASSWORD);

            when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(request.password(), mockUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Tests for findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return user when ID exists")
        void findById_Success() {
            UUID id = UUID.randomUUID();
            User mockUser = new User();
            mockUser.setId(id);

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            User result = userService.findById(id);

            assertThat(result).isEqualTo(mockUser);
        }

        @Test
        @DisplayName("Should throw exception when ID does not exist")
        void findById_NotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USR_NOT_FOUND)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }
    }
}
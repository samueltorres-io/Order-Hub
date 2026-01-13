package com.orderhub.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.entity.Role;
import com.orderhub.entity.User;
import com.orderhub.entity.UserRole;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.RoleRepository;
import com.orderhub.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_PATTERN = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";

    @Transactional
    public User create(Register req) {

        if (req.username() == null || req.username().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        if (req.email() == null || req.email().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        Optional<User> emailExist = userRepository.findByEmail(req.email());
        if (emailExist.isPresent()) {
            throw new AppException(ErrorCode.USR_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        if (req.password() == null || req.password().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        /**
         * Has minimum 8 characters in length.
         * At least one uppercase English letter.
         * At least one lowercase English letter.
         * At least one digit.
         * At least one special character.
         * 
        */
        boolean isMatch = Pattern.compile(PASSWORD_PATTERN)
                .matcher(req.password())
                .find();

        if (!isMatch) {
            throw new AppException(ErrorCode.WEAK_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        User user = new User();

        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));

        /* ----------- Migrar para outra funcção ----------- */
        Role defaultRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(defaultRole);
        userRole.getId().setUserId(user.getId());
        userRole.getId().setRoleId(defaultRole.getId());

        user.getRoles().add(userRole);
        /* ------------------------------------------------- */

        return userRepository.save(user);

    }

    @Transactional
    public User login(Login req) {

        if (req.email() == null || req.email().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        if (req.password() == null || req.password().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        Optional<User> userOptional = userRepository.findByEmail(req.email());
        if (userOptional.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        User user = userOptional.get();

        boolean isPasswordValid = passwordEncoder.matches(req.password(), user.getPasswordHash());
        
        if (!isPasswordValid) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        return user;

    }

}

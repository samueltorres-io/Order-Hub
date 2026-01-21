package com.orderhub.service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.entity.User;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
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

        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

        
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        return user;

    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

}

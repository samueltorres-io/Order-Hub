package com.orderhub.service;

import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.dto.auth.response.AuthResponse;
import com.orderhub.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;

    @Transactional
    private AuthResponse create(Register req) {

        User savedUser = userService.create(req);

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.fromEntity(savedUser, token, null);

    }

    @Transactional
    private AuthResponse login(Login req) {

        User user = userService.login(req);

        String token = jwtService.generateToken(user);

        return AuthResponse.fromEntity(user, token, null);

    }

}

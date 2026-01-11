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

    @Transactional
    private AuthResponse create(Register req) {

        User savedUser = userService.create(req);

        /* Geração de tokens ... */

        return AuthResponse.fromEntity(savedUser, null, null);

    }

    @Transactional
    private AuthResponse login(Login req) {

        User user = userService.login(req);

        /* Geração de tokens */

        return AuthResponse.fromEntity(user, null, null);

    }

}

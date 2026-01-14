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
    private final RoleService roleService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse create(Register req) {

        User savedUser = userService.create(req);

        roleService.associateRole(savedUser.getId(), "USER");

        /* Refresh de user com roles, para não quebrar geração de tokens com roles */
        User savedUserWithRoles = userService.findById(savedUser.getId());

        String accessToken = jwtService.generateAccessToken(savedUserWithRoles);
        String refreshToken = jwtService.generateRefreshToken();

        /* Salvar refresh token no redis (elasticache aws) */

        return AuthResponse.fromEntity(savedUserWithRoles, accessToken, refreshToken);

    }

    @Transactional
    public AuthResponse login(Login req) {

        User user = userService.login(req);

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = jwtService.generateRefreshToken();

        /* Salvar refresh token no redis (elasticache aws) */

        return AuthResponse.fromEntity(user, accessToken, refreshToken);

    }

}

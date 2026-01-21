package com.orderhub.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.RefreshRequest;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.dto.auth.response.AuthResponse;
import com.orderhub.dto.auth.response.TokenResponse;
import com.orderhub.entity.User;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final JwtService jwtService;
    private final RedisService redisService;

    @Transactional
    public AuthResponse create(Register req) {

        User savedUser = userService.create(req);

        roleService.associateRole(savedUser.getId(), "USER");

        User user = userService.findById(savedUser.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        redisService.save(refreshToken, user.getId().toString());

        return AuthResponse.fromEntity(user, accessToken, refreshToken);

    }

    @Transactional
    public AuthResponse login(Login req) {

        User user = userService.login(req);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        redisService.save(refreshToken, user.getId().toString());

        return AuthResponse.fromEntity(user, accessToken, refreshToken);

    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {

        String userId = redisService.get(req.refreshToken());
        if (userId == null) throw new AppException(ErrorCode.INVALID_TOKEN, HttpStatus.BAD_REQUEST);

        User user = userService.findById(UUID.fromString(userId));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken();
        
        redisService.delete(req.refreshToken());
        redisService.save(newRefreshToken, user.getId().toString());

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}

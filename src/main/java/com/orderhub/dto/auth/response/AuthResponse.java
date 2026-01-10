package com.orderhub.dto.auth.response;

import java.util.UUID;

import com.orderhub.entity.User;

public record AuthResponse(

    UUID id,
    String username,
    String email,
    String accessToken,
    String refreshToken

) {

    public static AuthResponse fromEntity(User user, String accessToken, String refreshToken) {

        return new AuthResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            accessToken,
            refreshToken
        );

    }

}

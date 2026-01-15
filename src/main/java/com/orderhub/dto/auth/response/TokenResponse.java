package com.orderhub.dto.auth.response;

public record TokenResponse(

    String accessToken,
    String refreshToken

) {}

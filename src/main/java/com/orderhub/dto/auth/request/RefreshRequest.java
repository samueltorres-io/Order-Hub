package com.orderhub.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

    @NotBlank(message = "Request Token is mandatory")
    String refreshToken

) {}

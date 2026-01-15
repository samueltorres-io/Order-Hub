package com.orderhub.dto.role.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssociateUnlink(
    
    @NotNull(message = "UserId is mandatory")
    UUID userId,

    @NotBlank(message = "Role name is mandatory")
    String roleName

) {}

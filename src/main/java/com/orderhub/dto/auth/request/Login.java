package com.orderhub.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Login(

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email address cannot be invalid")
    @Size(min = 3, max = 64, message = "Email must be between 3 and 64 characters long")
    String email,

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters long")
    String password

) {}

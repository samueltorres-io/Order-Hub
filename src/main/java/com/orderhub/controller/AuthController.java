package com.orderhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.RefreshRequest;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.dto.auth.response.AuthResponse;
import com.orderhub.dto.auth.response.TokenResponse;
import com.orderhub.dto.error.ApiError;
import com.orderhub.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth Routes", description = "Routes for register, login and generate new token pair (access/refresh)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Register a new user to the system and returns the authentication token")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "User created successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    value = "{\"success\":false,\"errorCode\":\"ERR_INVALID_INPUT\",\"status\":400,\"message\":\"Invalid request data\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"abc-123\",\"details\":[\"email: must be a well-formed email address\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "User already exists",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    value = "{\"success\":false,\"errorCode\":\"ERR_USER_ALREADY_EXISTS\",\"status\":409,\"message\":\"User already exists\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"xyz-789\",\"details\":null}"
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> create(@Valid @RequestBody Register req) {
        var response = authService.create(req);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
                
        return ResponseEntity.created(uri).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody Login req) {
        var response = authService.login(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        TokenResponse response = authService.refresh(req);
        return ResponseEntity.ok(response);
    }

}
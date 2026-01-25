package com.orderhub.controller;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.orderhub.dto.error.ApiError;
import com.orderhub.dto.role.request.AssociateUnlink;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Associate Role to User", description = "Grant a specific role (e.g., ADMIN, MANAGER) to a user. Requires ADMIN privileges.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Role associated successfully"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Insufficient privileges",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Forbidden",
                    summary = "User is not an Admin",
                    value = "{\"success\":false,\"errorCode\":\"ERR_UNAUTHORIZED\",\"status\":403,\"message\":\"Access denied\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"sec-001\",\"details\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Resource not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = {
                    @ExampleObject(
                        name = "User Not Found",
                        summary = "Target user ID does not exist",
                        value = "{\"success\":false,\"errorCode\":\"ERR_USER_NOT_FOUND\",\"status\":404,\"message\":\"User not found\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"abc-111\",\"details\":null}"
                    ),
                    @ExampleObject(
                        name = "Role Not Found",
                        summary = "Role does not exist (Masked as Invalid Input)",
                        value = "{\"success\":false,\"errorCode\":\"ERR_INVALID_INPUT\",\"status\":404,\"message\":\"Invalid input provided\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"abc-222\",\"details\":null}"
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Conflict",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Association Exists",
                    summary = "User already has this role",
                    value = "{\"success\":false,\"errorCode\":\"ERR_ASSOCIATION_ALREADY_EXISTS\",\"status\":409,\"message\":\"Association already exists\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"conf-333\",\"details\":null}"
                )
            )
        )
    })
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Void> associate(
        @PathVariable UUID id,
        @RequestBody AssociateUnlink req,
        Authentication authentication
    ) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID adminId = UUID.fromString(jwt.getClaim("id").toString());

        if (roleService.verifyRole(adminId, "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }

        roleService.associateRole(id, req.roleName());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Void> unlink(
        @PathVariable UUID id,
        @PathVariable String roleName,
        Authentication authentication
    ) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID adminId = UUID.fromString(jwt.getClaim("id").toString());

        if (!roleService.verifyRole(adminId, "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }

        roleService.unlinkRole(id, roleName);
        
        return ResponseEntity.noContent().build();
    }

}
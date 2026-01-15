package com.orderhub.controller;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.orderhub.dto.role.request.AssociateUnlink;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.service.RoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class RoleController {

    private final RoleService roleService;

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
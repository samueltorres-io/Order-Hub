package com.orderhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.orderhub.dto.auth.request.Login;
import com.orderhub.dto.auth.request.Register;
import com.orderhub.dto.auth.response.AuthResponse;
import com.orderhub.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public ResponseEntity<AuthResponse> create(Register req) {
        var response = authService.create(req);
        return ResponseEntity.created(
            /* https://cursos.alura.com.br/forum/topico-alternativa-para-pegar-a-uri-264657 */
            ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri())
            .body(response);
    }

    public ResponseEntity<AuthResponse> login(Login req) {
        var response = authService.login(req);
        return ResponseEntity.ok(response);
    }

}

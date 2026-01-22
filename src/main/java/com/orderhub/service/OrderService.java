package com.orderhub.service;

import org.springframework.stereotype.Service;

import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.entity.User;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;

    public void create(CreateOrderRequest req) {

        /* Vaildar se o usuário existe */

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND));

        /* Validar se os produtos existem */

        /* Montar o objeto Order */

        /* Montar o objeto Outbox com o JSON do pedido */

        /* Salvar os dois */

    }

}

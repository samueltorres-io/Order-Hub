package com.orderhub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Register;
import com.orderhub.dto.auth.response.AuthResponse;
import com.orderhub.entity.User;
import com.orderhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private AuthResponse createUser(Register req) {
        
        User user = new User();

        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));

        User savedUser = userRepository.save(user);

        /* Geração de tokens ... */

        return AuthResponse.fromEntity(savedUser, null, null);

    }

}

package com.orderhub.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderhub.dto.auth.request.Register;
import com.orderhub.entity.User;
import com.orderhub.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(Register req) {

        if (req.username().isBlank() || req.username().isEmpty()) {
            /* throw new AppError... */
        }

        if (req.email().isBlank() || req.email().isEmpty()) {
            /* throw new AppError... */
        }

        Optional<User> emailExist = userRepository.existsByEmail(req.email());
        if (!emailExist.isEmpty()) {
            /* throw new AppError... credenciais inválidas para evitar dizer que email já existe no serviço */
        }

        if (req.password().isBlank() || req.password().isEmpty()) {
            /* throw new AppError... */
        }

        /**
         * Has minimum 8 characters in length.
         * At least one uppercase English letter.
         * At least one lowercase English letter.
         * At least one digit.
         * At least one special character.
         * 
        */
        boolean isMatch = Pattern.compile("/^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$/")
                .matcher(req.password())
                .find();

        if (!isMatch) {
            /* throw new AppError */
        }

        User user = new User();

        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));

        return userRepository.save(user);

    }

}

package com.orderhub.service;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.orderhub.entity.User;

@Service
public class JwtService {
    private final JwtEncoder encoder;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiresAt = 36000L;

        String scope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("spring-security-jwt")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiresAt))
            .subject(authentication.getName())
            .claim("scopes", scope)
            .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        long expiresAt = 36000L;

        String scope = "USER"; 

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("orderhub-api")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiresAt))
            .subject(user.getId().toString())
            .claim("id", user.getId())
            .claim("scope", scope)
            .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
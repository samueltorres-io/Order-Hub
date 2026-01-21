package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.orderhub.entity.Role;
import com.orderhub.entity.User;
import com.orderhub.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder encoder;

    @InjectMocks
    private JwtService jwtService;

    private Jwt createMockJwt(String tokenValue) {
        return new Jwt(
            tokenValue, 
            Instant.now(), 
            Instant.now().plusSeconds(300), 
            Map.of("alg", "none"), 
            Map.of("sub", "test")
        );
    }

    @Test
    @DisplayName("Should generate token from Authentication object correctly")
    void generateToken_FromAuthentication() {

        String expectedTokenValue = "mocked-jwt-token";
        String username = "admin-user";

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        
        GrantedAuthority authority = new SimpleGrantedAuthority("SCOPE_ADMIN");
        
        doReturn(Set.of(authority)).when(auth).getAuthorities();

        when(encoder.encode(any(JwtEncoderParameters.class)))
            .thenReturn(createMockJwt(expectedTokenValue));

        String result = jwtService.generateToken(auth);

        assertThat(result).isEqualTo(expectedTokenValue);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());

        JwtClaimsSet capturedClaims = captor.getValue().getClaims();

        assertThat(capturedClaims.getSubject()).isEqualTo(username);
        
        assertThat((String) capturedClaims.getClaim("iss")).isEqualTo("spring-security-jwt");
        
        assertThat((String) capturedClaims.getClaim("scopes")).isEqualTo("SCOPE_ADMIN");
        assertThat(capturedClaims.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Should generate access token from User entity correctly")
    void generateAccessToken_FromUser() {

        String expectedTokenValue = "user-access-token";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        
        Role roleAdmin = new Role();
        roleAdmin.setName("ROLE_ADMIN");
        
        UserRole userRole = new UserRole();
        userRole.setRole(roleAdmin);
        userRole.setUser(user);
        
        user.setRoles(List.of(userRole));

        when(encoder.encode(any(JwtEncoderParameters.class)))
            .thenReturn(createMockJwt(expectedTokenValue));

        String result = jwtService.generateAccessToken(user);

        assertThat(result).isEqualTo(expectedTokenValue);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        
        assertThat((String) claims.getClaim("iss")).isEqualTo("orderhub-api");
        
        assertThat((UUID) claims.getClaim("id")).isEqualTo(userId);
        
        assertThat((String) claims.getClaim("scope")).isEqualTo("USER");
        
        List<String> rolesClaim = claims.getClaim("roles");
        assertThat(rolesClaim).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should generate access token even if user has no roles")
    void generateAccessToken_NoRoles() {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRoles(null);

        when(encoder.encode(any(JwtEncoderParameters.class)))
            .thenReturn(createMockJwt("token"));

        jwtService.generateAccessToken(user);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();
        List<String> roles = claims.getClaim("roles");
        
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("Should generate a valid random UUID for refresh token")
    void generateRefreshToken() {
        String token = jwtService.generateRefreshToken();

        assertThat(token).isNotNull();
        assertThat(UUID.fromString(token)).isNotNull();
    }
}
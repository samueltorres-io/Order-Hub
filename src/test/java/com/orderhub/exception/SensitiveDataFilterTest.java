package com.orderhub.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataFilterTest {

    @Test
    @DisplayName("Should return null when input is null")
    void sanitize_Null() {
        assertThat(SensitiveDataFilter.sanitize(null)).isNull();
    }

    @Test
    @DisplayName("Should redact email addresses")
    void sanitize_Email() {
        String input = "User login: john.doe@example.com attempted access.";
        String expected = "User login: EMAIL_REDACTED attempted access.";

        assertThat(SensitiveDataFilter.sanitize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should redact JWT tokens")
    void sanitize_Jwt() {

        String input = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        String result = SensitiveDataFilter.sanitize(input);
        
        assertThat(result).contains("JWT_TOKEN_REDACTED");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    @DisplayName("Should keep safe text unchanged")
    void sanitize_SafeText() {
        String input = "User updated profile successfully with ID 12345";
        assertThat(SensitiveDataFilter.sanitize(input)).isEqualTo(input);
    }
}
package com.orderhub.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AppExceptionTest {

    @Test
    @DisplayName("Should create exception with specific message")
    void constructor_WithMessage() {
        AppException ex = new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST, "Custom Message");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Custom Message");
    }

    @Test
    @DisplayName("Should create exception with default message from ErrorCode")
    void constructor_DefaultMessage() {
        AppException ex = new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USR_NOT_FOUND);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.USR_NOT_FOUND.getDefaultMessage());
    }
}
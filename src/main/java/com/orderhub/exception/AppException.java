package com.orderhub.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AppException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public AppException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.status = status;
    }

    public HttpStatus geHttpStatus() {
        return status;
    }

}

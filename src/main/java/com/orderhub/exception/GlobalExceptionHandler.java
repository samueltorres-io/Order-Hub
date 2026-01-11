package com.orderhub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.orderhub.dto.error.ApiError;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    private boolean isDevelopment() {
        return "dev".equalsIgnoreCase(activeProfile) || "local".equalsIgnoreCase(activeProfile);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status, 
            String errorCode, 
            String message,
            Object details
    ) {
        String traceId = generateTraceId();
        
        ApiError body = new ApiError(
            false,
            errorCode,
            status.value(),
            message,
            Instant.now(),
            traceId,
            isDevelopment() ? details : null
        );
        
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex) {
        String traceId = generateTraceId();
        
        if (log.isDebugEnabled()) {
            log.debug("AppException details [traceId={}]: {}", traceId, ex.getMessage(), ex);
        }
        
        ApiError body = new ApiError(
            false,
            ex.getErrorCode().getCode(),
            ex.getStatus().value(),
            ex.getMessage(),
            Instant.now(),
            traceId,
            null
        );
        
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = generateTraceId();
        
        log.warn("Validation error [traceId={}] - {} field(s) invalid", 
            traceId, 
            ex.getBindingResult().getFieldErrorCount());
        
        if (isDevelopment()) {
            List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format("%s: %s", e.getField(), e.getDefaultMessage()))
                .collect(Collectors.toList());
            
            return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_INPUT.getCode(),
                "Validation failed",
                errors
            );
        } else {
            return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_INPUT.getCode(),
                "Invalid request data. Please check your input and try again.",
                null
            );
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException ex) {
        String traceId = generateTraceId();
        log.warn("Malformed JSON [traceId={}]", traceId);
        
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.MALFORMED_JSON.getCode(),
            "Invalid JSON format in request body",
            isDevelopment() ? ex.getMostSpecificCause().getMessage() : null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        String traceId = generateTraceId();
        log.warn("Missing parameter [traceId={}] [param={}]", traceId, ex.getParameterName());
        
        String message = isDevelopment() 
            ? String.format("Required parameter '%s' is missing", ex.getParameterName())
            : "Required parameter is missing";
        
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.MISSING_PARAMETER.getCode(),
            message,
            null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String traceId = generateTraceId();
        log.warn("Type mismatch [traceId={}] [param={}]", traceId, ex.getName());
        
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INPUT.getCode(),
            "Invalid parameter type",
            null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String traceId = generateTraceId();
        log.warn("Method not allowed [traceId={}] [method={}]", traceId, ex.getMethod());
        
        return buildResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            ErrorCode.METHOD_NOT_ALLOWED.getCode(),
            "HTTP method not allowed for this endpoint",
            null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        String traceId = generateTraceId();
        log.warn("Access denied [traceId={}]", traceId);
        
        return buildResponse(
            HttpStatus.FORBIDDEN,
            ErrorCode.FORBIDDEN.getCode(),
            "You don't have permission to access this resource",
            null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        String traceId = generateTraceId();
        
        log.error("Unexpected error [traceId={}] [type={}]", 
            traceId, 
            ex.getClass().getSimpleName(), 
            ex);
        
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "An unexpected error occurred. Please try again later or contact support with trace ID: " + traceId,
            isDevelopment() ? ex.getMessage() : null
        );
    }
}
package com.orderhub.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    /* Generics */
    INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER_ERROR", "Internal server error"),
    INVALID_INPUT("ERR_INVALID_INPUT", "Invalid input data"),
    NOT_FOUND("ERR_NOT_FOUND", "Resource not found"),
    GONE("ERR_GONE", "Resource no longer available"),
    BAD_REQUEST("ERR_BAD_REQUEST", "..."),

    /* User & Auth */
    USR_ALREADY_EXISTS("ERR_USER_ALREADY_EXISTS", "User already exists"),
    USR_NOT_FOUND("ERR_USER_NOT_FOUND", "User not found"),
    UNAUTHORIZED("ERR_UNAUTHORIZED", "Unauthorized access"),
    INVALID_CREDENTIALS("ERR_INVALID_CREDENTIALS", "Invalid Credentials"),
    INVALID_TOKEN("ERR_INVALID_TOKEN", "Invalid or expired token"),
    TOKEN_EXPIRED("ERR_TOKEN_EXPIRED", "Token expired"),

    /* Specific HTTP */
    MALFORMED_JSON("ERR_MALFORMED_JSON", "Malformed JSON request body"),
    MISSING_PARAMETER("ERR_MISSING_PARAMETER", "Required parameter is missing"),
    METHOD_NOT_ALLOWED("ERR_METHOD_NOT_ALLOWED", "HTTP method not allowed"),
    UNSUPPORTED_MEDIA_TYPE("ERR_UNSUPPORTED_MEDIA_TYPE", "Unsupported media type"),

    /* Rate Limiting & Security */
    TOO_MANY_REQUESTS("ERR_TOO_MANY_REQUESTS", "Too many requests"),
    FORBIDDEN("ERR_FORBIDDEN", "Access forbidden");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }


}

package com.orderhub.exception;

import java.util.regex.Pattern;

public class SensitiveDataFilter {

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern BASE64_KEY_PATTERN = Pattern.compile(
        "[A-Za-z0-9+/]{40,}={0,2}"
    );

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input;
        
        sanitized = JWT_PATTERN.matcher(sanitized)
            .replaceAll("JWT_TOKEN_REDACTED");
        
        sanitized = EMAIL_PATTERN.matcher(sanitized)
            .replaceAll("EMAIL_REDACTED");
        
        sanitized = BASE64_KEY_PATTERN.matcher(sanitized)
            .replaceAll("KEY_REDACTED");
        
        return sanitized;
    }

}

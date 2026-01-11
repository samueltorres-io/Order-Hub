package com.orderhub.dto.error;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    @JsonProperty("success") boolean success,
    @JsonProperty("errorCode") String errorCode,
    @JsonProperty("status") int status,
    @JsonProperty("message") String message,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("traceId") String traceId,
    @JsonProperty("details") Object details
) {
    public ApiError(boolean success, String errorCode, int status, String message, Instant timestamp, String traceId) {
        this(success, errorCode, status, message, timestamp, traceId, null);
    }
}

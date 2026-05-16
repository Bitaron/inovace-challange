package com.innovace.Innovacechallenge.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Structured error envelope returned for all non-2xx responses.
 *
 * <pre>
 * Error Code Catalogue:
 *   4001 – Request validation failed (missing/invalid fields)
 *   5001 – LLM API call failed (network, auth, quota)
 *   5002 – LLM returned non-parseable JSON
 *   5003 – Configured LLM provider is not registered
 *   5099 – Unexpected internal server error
 * </pre>
 */
@Getter
@Builder
public class ApiError {

    /** Application-specific numeric error code. */
    private final int errorCode;

    /** Short, human-readable description of the error. */
    private final String message;

    /** Additional diagnostic detail (validation field errors, upstream error text, etc.). May be null. */
    private final String details;

    /** UTC timestamp when the error occurred. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private final Instant timestamp = Instant.now();
}

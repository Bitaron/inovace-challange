package com.innovace.Innovacechallenge.exception;

/**
 * Thrown when an LLM provider call fails at the transport or API level
 * (e.g., network error, authentication failure, quota exceeded).
 *
 * <p>Maps to HTTP 500 with error code {@code 5001}.
 */
public class LlmCallException extends RuntimeException {

    public LlmCallException(String message) {
        super(message);
    }

    public LlmCallException(String message, Throwable cause) {
        super(message, cause);
    }
}

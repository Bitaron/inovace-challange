package com.innovace.Innovacechallenge.exception;

/**
 * Thrown when the LLM responds successfully but its output cannot be parsed
 * as the expected JSON structure {@code LogSummaryResponse}.
 *
 * <p>Maps to HTTP 500 with error code {@code 5002}.
 */
public class LlmParsingException extends RuntimeException {

    private final String rawResponse;

    public LlmParsingException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    /** Returns the raw, unparseable LLM output for diagnostic purposes. */
    public String getRawResponse() {
        return rawResponse;
    }
}

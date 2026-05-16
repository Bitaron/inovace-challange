package com.innovace.Innovacechallenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handler for all controllers.
 *
 * <p>Maps each exception type to an {@link ApiError} with a specific error code and
 * HTTP status. No raw exception details are leaked to the caller; only the
 * {@code details} field may include diagnostic information appropriate for clients.
 *
 * <table border="1">
 *   <caption>Error code mapping</caption>
 *   <tr><th>Exception</th><th>HTTP</th><th>Code</th></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>4001</td></tr>
 *   <tr><td>LlmCallException</td><td>500</td><td>5001</td></tr>
 *   <tr><td>LlmParsingException</td><td>500</td><td>5002</td></tr>
 *   <tr><td>UnsupportedLlmProviderException</td><td>500</td><td>5003</td></tr>
 *   <tr><td>Exception (catch-all)</td><td>500</td><td>5099</td></tr>
 * </table>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 4xx — Client errors
    // -----------------------------------------------------------------------

    /**
     * Handles Bean Validation failures on the request body.
     * Collects all field-level error messages into the {@code details} string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed: {}", details);

        return ResponseEntity.badRequest().body(ApiError.builder()
                .errorCode(4001)
                .message("Request validation failed")
                .details(details)
                .build());
    }

    // -----------------------------------------------------------------------
    // 5xx — Server / LLM errors
    // -----------------------------------------------------------------------

    /**
     * Handles failures during the LLM API call (network, auth, quota, etc.).
     */
    @ExceptionHandler(LlmCallException.class)
    public ResponseEntity<ApiError> handleLlmCall(LlmCallException ex) {
        log.error("LLM API call failed", ex);

        return ResponseEntity.internalServerError().body(ApiError.builder()
                .errorCode(5001)
                .message("LLM API call failed. The upstream model service is unavailable or rejected the request.")
                .details(ex.getMessage())
                .build());
    }

    /**
     * Handles LLM responses that cannot be deserialized into {@code LogSummaryResponse}.
     */
    @ExceptionHandler(LlmParsingException.class)
    public ResponseEntity<ApiError> handleLlmParsing(LlmParsingException ex) {
        log.error("LLM response parsing failed. Raw response: {}", ex.getRawResponse(), ex);

        return ResponseEntity.internalServerError().body(ApiError.builder()
                .errorCode(5002)
                .message("LLM returned a response that could not be parsed as structured JSON.")
                .details(ex.getMessage())
                .build());
    }

    /**
     * Handles misconfigured {@code app.llm.provider} values.
     */
    @ExceptionHandler(UnsupportedLlmProviderException.class)
    public ResponseEntity<ApiError> handleUnsupportedProvider(UnsupportedLlmProviderException ex) {
        log.error("Unsupported LLM provider configured: {}", ex.getRequestedProvider());

        return ResponseEntity.internalServerError().body(ApiError.builder()
                .errorCode(5003)
                .message("The configured LLM provider is not supported.")
                .details(ex.getMessage())
                .build());
    }

    /**
     * Catch-all handler for unexpected runtime errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Unexpected internal error", ex);

        return ResponseEntity.internalServerError().body(ApiError.builder()
                .errorCode(5099)
                .message("An unexpected internal error occurred. Please try again later.")
                .details(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                .build());
    }
}

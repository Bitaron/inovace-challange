package com.innovace.Innovacechallenge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single application log line submitted for analysis.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "timestamp": "2025-10-15T10:00:05Z",
 *   "level":     "ERROR",
 *   "service":   "payment-service",
 *   "message":   "Database connection timed out after 3001ms"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    @NotBlank(message = "timestamp must not be blank")
    private String timestamp;

    @NotBlank(message = "level must not be blank")
    private String level;

    @NotBlank(message = "service must not be blank")
    private String service;

    @NotBlank(message = "message must not be blank")
    private String message;
}

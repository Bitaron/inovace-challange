package com.innovace.Innovacechallenge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured anomaly report returned by {@code POST /summarize-logs}.
 *
 * <p>This DTO is dual-purpose:
 * <ul>
 *   <li><b>Deserialization</b>: parsed from the LLM's raw JSON output (snake_case fields).</li>
 *   <li><b>Serialization</b>: returned as the HTTP response body (same snake_case convention).</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * {
 *   "summary": "A database connectivity issue in payment-service is causing cascading failures.",
 *   "key_error_signatures": ["Database connection timed out", "Failed to process payment"],
 *   "recommendation": "Investigate database load and network latency for the payment-service.",
 *   "analyzed_log_count": 15
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogSummaryResponse {

    /** One-paragraph narrative of the root cause or system health status. */
    private String summary;

    /** Distinct error/warning patterns identified in the logs. */
    @JsonProperty("key_error_signatures")
    private List<String> keyErrorSignatures;

    /** Concrete, actionable next steps for an on-call engineer. */
    private String recommendation;

    /**
     * Total number of log entries that were analyzed.
     * Overwritten by the service with the actual request count to prevent LLM hallucinations.
     */
    @JsonProperty("analyzed_log_count")
    private int analyzedLogCount;
}

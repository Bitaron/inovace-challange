package com.innovace.Innovacechallenge.controller;

import com.innovace.Innovacechallenge.dto.LogSummaryRequest;
import com.innovace.Innovacechallenge.dto.LogSummaryResponse;
import com.innovace.Innovacechallenge.service.LogSummarizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the log summarization endpoint.
 *
 * <p>This controller is intentionally thin — all business logic lives in
 * {@link LogSummarizerService}. Exceptions propagate to
 * {@link com.innovace.Innovacechallenge.exception.GlobalExceptionHandler}.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class LogSummarizerController {

    private final LogSummarizerService logSummarizerService;

    /**
     * Accepts a batch of application log entries, sends them to the configured LLM,
     * and returns a structured anomaly report.
     *
     * <p><b>POST /summarize-logs</b>
     *
     * <p>Request body example:
     * <pre>
     * {
     *   "logs": [
     *     {
     *       "timestamp": "2025-10-15T10:00:05Z",
     *       "level":     "ERROR",
     *       "service":   "payment-service",
     *       "message":   "Database connection timed out after 3001ms"
     *     }
     *   ]
     * }
     * </pre>
     *
     * @param request validated request body containing the log entries
     * @return 200 OK with a {@link LogSummaryResponse}
     */
    @PostMapping("/summarize-logs")
    public ResponseEntity<LogSummaryResponse> summarizeLogs(
            @Valid @RequestBody LogSummaryRequest request) {

        log.info("POST /summarize-logs — received {} log entries", request.getLogs().size());
        LogSummaryResponse response = logSummarizerService.summarize(request.getLogs());
        return ResponseEntity.ok(response);
    }
}

package com.innovace.Innovacechallenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovace.Innovacechallenge.dto.LogEntry;
import com.innovace.Innovacechallenge.dto.LogSummaryResponse;
import com.innovace.Innovacechallenge.exception.LlmParsingException;
import com.innovace.Innovacechallenge.llm.LlmClient;
import com.innovace.Innovacechallenge.llm.LlmClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core business service for the LogSummarizer.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Resolve the active {@link LlmClient} from the factory.</li>
 *   <li>Build the system prompt and user message.</li>
 *   <li>Invoke the LLM and parse its JSON response.</li>
 *   <li>Guarantee {@code analyzedLogCount} matches the actual request size.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSummarizerService {

    // -----------------------------------------------------------------------
    // Engineered system prompt
    // -----------------------------------------------------------------------
    /**
     * System-level instructions sent to every LLM call.
     *
     * <p>Design goals:
     * <ul>
     *   <li><b>Role framing</b>: positions the model as an SRE specialist to elicit
     *       domain-specific reasoning.</li>
     *   <li><b>Strict JSON-only output</b>: prevents markdown fences or prose that
     *       would break JSON deserialization.</li>
     *   <li><b>Explicit schema</b>: reduces hallucination of unknown fields.</li>
     *   <li><b>Low temperature</b>: configured in properties (0.2) for deterministic output.</li>
     * </ul>
     */
    static final String SYSTEM_PROMPT = """
            You are an expert Site Reliability Engineer (SRE) specialising in application log analysis and anomaly detection.

            CRITICAL OUTPUT RULES — follow these exactly:
            1. Your ENTIRE response MUST be a single, valid JSON object.
            2. Do NOT include markdown code fences (```json or ```), explanations, or any text outside the JSON object.
            3. The JSON MUST conform strictly to this schema:
               {
                 "summary":              "<string: one-paragraph narrative of the root cause or system health>",
                 "key_error_signatures": ["<string: distinct error pattern>", ...],
                 "recommendation":       "<string: concrete, actionable next steps for an on-call engineer>",
                 "analyzed_log_count":   <integer: exact number of log entries you received>
               }
            4. All four fields are required. Do not omit or rename them.

            ANALYSIS INSTRUCTIONS:
            - Identify recurring ERROR or WARN messages and group them into distinct error signatures.
            - Look for causal chains across services (e.g., database timeouts → downstream payment failures).
            - Detect anomalies: bursts of errors from a single service, mixed severity spikes, or unusual timing patterns.
            - Note which microservices are affected and whether failures appear isolated or cascading.
            - If all logs are INFO level with no issues, state "System appears healthy" in the summary and return an empty array for key_error_signatures.
            - Be concise but precise: include service names, error types, and relevant time windows.
            """;

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final LlmClientFactory llmClientFactory;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Analyses the provided log entries and returns a structured anomaly report.
     *
     * @param logs non-empty list of log entries (already validated by the controller)
     * @return structured summary from the LLM
     * @throws com.innovace.Innovacechallenge.exception.LlmCallException    if the LLM API call fails
     * @throws LlmParsingException if the LLM response cannot be parsed
     */
    public LogSummaryResponse summarize(List<LogEntry> logs) {
        log.info("Starting log analysis for {} log entries", logs.size());

        LlmClient client = llmClientFactory.getClient();
        String userMessage = buildUserMessage(logs);

        String rawResponse = client.call(SYSTEM_PROMPT, userMessage);
        log.debug("Raw LLM response: {}", rawResponse);

        LogSummaryResponse response = parseResponse(rawResponse, logs.size());

        // Override LLM's count with the authoritative value from the request
        response.setAnalyzedLogCount(logs.size());

        log.info("Log analysis complete — provider: {}, logs analyzed: {}",
                client.getProviderName(), logs.size());
        return response;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the user-facing message that contains the formatted log lines.
     */
    private String buildUserMessage(List<LogEntry> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following ").append(logs.size())
          .append(" application log entries and return the JSON anomaly report.\n\n")
          .append("<LOGS>\n");

        for (int i = 0; i < logs.size(); i++) {
            LogEntry entry = logs.get(i);
            sb.append(String.format("[%d] %s | %-5s | %-30s | %s%n",
                    i + 1,
                    entry.getTimestamp(),
                    entry.getLevel(),
                    entry.getService(),
                    entry.getMessage()));
        }

        sb.append("</LOGS>");
        return sb.toString();
    }

    /**
     * Parses the raw LLM text response into a {@link LogSummaryResponse}.
     *
     * @param rawResponse   raw text from the LLM
     * @param expectedCount number of logs that were analyzed (used for validation)
     * @throws LlmParsingException if parsing fails
     */
    private LogSummaryResponse parseResponse(String rawResponse, int expectedCount) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new LlmParsingException(
                    "LLM returned an empty response", rawResponse, null);
        }

        // Strip accidental markdown fences that some models add despite instructions
        String cleaned = rawResponse.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("```\\s*$", "").strip();
        }

        try {
            return objectMapper.readValue(cleaned, LogSummaryResponse.class);
        } catch (Exception ex) {
            log.error("Failed to parse LLM response as JSON. Raw response: {}", rawResponse, ex);
            throw new LlmParsingException(
                    "LLM response could not be parsed as valid JSON", rawResponse, ex);
        }
    }
}

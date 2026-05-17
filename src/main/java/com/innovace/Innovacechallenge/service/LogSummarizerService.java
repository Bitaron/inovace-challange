package com.innovace.Innovacechallenge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovace.Innovacechallenge.dto.LogEntry;
import com.innovace.Innovacechallenge.dto.LogSummaryResponse;
import com.innovace.Innovacechallenge.exception.LlmParsingException;
import com.innovace.Innovacechallenge.llm.LlmClient;
import com.innovace.Innovacechallenge.llm.LlmClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class LogSummarizerService {

    @Autowired
    private LlmClientFactory llmClientFactory;
    private ObjectMapper objectMapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Analyses the provided log entries and returns a structured anomaly report.
     *
     * @param logs non-empty list of log entries (already validated by the controller)
     * @return structured summary from the LLM
     * @throws com.innovace.Innovacechallenge.exception.LlmCallException if the LLM API call fails
     * @throws LlmParsingException                                       if the LLM response cannot be parsed
     */
    public LogSummaryResponse summarize(List<LogEntry> logs) throws JsonProcessingException {
        log.info("Starting log analysis for {} log entries", logs.size());

        LlmClient client = llmClientFactory.getClient();
        String userMessage = buildUserMessage(logs);

        log.info("Raw LLM request: {}", userMessage);
        String rawResponse = client.call(SystemPrompt.SYSTEM_PROMPT, userMessage);
        log.info("Raw LLM response: {}", rawResponse);

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
    private String buildUserMessage(List<LogEntry> logs) throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following ").append(logs.size())
                .append(" application log entries and return the JSON anomaly report.\n\n")
                .append("<input>\n")
                .append(objectMapper.writeValueAsString(logs))
                .append("</input>");
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

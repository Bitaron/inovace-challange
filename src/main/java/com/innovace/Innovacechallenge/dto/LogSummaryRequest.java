package com.innovace.Innovacechallenge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request body for {@code POST /summarize-logs}.
 */
@Data
public class LogSummaryRequest {

    @NotNull(message = "logs array must not be null")
    @NotEmpty(message = "logs array must contain at least one log entry")
    @Valid
    private List<LogEntry> logs;
}

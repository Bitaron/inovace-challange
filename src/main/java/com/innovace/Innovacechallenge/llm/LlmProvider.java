package com.innovace.Innovacechallenge.llm;

import lombok.Getter;

/**
 * Enum representing all supported LLM providers.
 *
 * <p>Each constant exposes a {@code key} that must match the value of
 * {@code app.llm.provider} in {@code application.properties} and the string
 * returned by the corresponding {@link LlmClient#getProviderName()} implementation.
 */
@Getter
public enum LlmProvider {

    /** Google Gemini via Spring AI Google GenAI starter. */
    GEMINI("gemini");

    /** Lower-case property key used for provider lookup. */
    private final String key;

    LlmProvider(String key) {
        this.key = key;
    }
}

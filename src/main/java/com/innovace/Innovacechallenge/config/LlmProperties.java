package com.innovace.Innovacechallenge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the LLM provider selection.
 *
 * <p>Bound from the {@code app.llm.*} prefix in {@code application.properties}.
 *
 * <pre>
 * # application.properties
 * app.llm.provider=gemini
 * </pre>
 *
 * <p>The {@code provider} value must match {@link com.innovace.Innovacechallenge.llm.LlmProvider#getKey()}
 * for one of the registered {@link com.innovace.Innovacechallenge.llm.LlmClient} beans.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /**
     * The LLM provider to activate.
     * Must match the key returned by {@code LlmClient#getProviderName()}.
     * Defaults to {@code "gemini"}.
     */
    private String provider = "gemini";
}

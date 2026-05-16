package com.innovace.Innovacechallenge.llm;

/**
 * Model-agnostic abstraction for interacting with a Large Language Model.
 *
 * <p>Each concrete implementation wraps a specific LLM provider (e.g., Gemini, OpenAI).
 * The active implementation is selected at runtime by {@link LlmClientFactory} based on
 * the {@code app.llm.provider} property.
 *
 * <p>To add a new provider:
 * <ol>
 *   <li>Create a Spring {@code @Component} that implements this interface.</li>
 *   <li>Return a unique provider key from {@link #getProviderName()}.</li>
 *   <li>Add the key to {@link LlmProvider} enum.</li>
 *   <li>Set {@code app.llm.provider=<your-key>} in {@code application.properties}.</li>
 * </ol>
 */
public interface LlmClient {

    /**
     * Sends a prompt to the LLM and returns the raw text response.
     *
     * @param systemPrompt  instructions that set the model's behaviour and output format
     * @param userMessage   the actual content/query to be processed
     * @return              raw response string from the model
     * @throws com.innovace.Innovacechallenge.exception.LlmCallException if the upstream
     *         API call fails (network, auth, quota, etc.)
     */
    String call(String systemPrompt, String userMessage);

    /**
     * Returns the unique provider identifier for this implementation.
     * Must match the value configured in {@code app.llm.provider}.
     *
     * @return lower-case provider key, e.g. {@code "gemini"}
     */
    String getProviderName();
}

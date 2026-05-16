package com.innovace.Innovacechallenge.exception;

import java.util.Set;

/**
 * Thrown by {@link com.innovace.Innovacechallenge.llm.LlmClientFactory} when
 * {@code app.llm.provider} is set to a value that has no registered {@code LlmClient} bean.
 *
 * <p>Maps to HTTP 500 with error code {@code 5003}.
 */
public class UnsupportedLlmProviderException extends RuntimeException {

    private final String requestedProvider;
    private final Set<String> registeredProviders;

    public UnsupportedLlmProviderException(String requestedProvider, Set<String> registeredProviders) {
        super(String.format(
                "LLM provider '%s' is not registered. Available providers: %s",
                requestedProvider, registeredProviders));
        this.requestedProvider = requestedProvider;
        this.registeredProviders = registeredProviders;
    }

    public String getRequestedProvider() {
        return requestedProvider;
    }

    public Set<String> getRegisteredProviders() {
        return registeredProviders;
    }
}

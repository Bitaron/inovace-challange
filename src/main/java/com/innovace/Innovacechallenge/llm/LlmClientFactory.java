package com.innovace.Innovacechallenge.llm;

import com.innovace.Innovacechallenge.config.LlmProperties;
import com.innovace.Innovacechallenge.exception.UnsupportedLlmProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves the active {@link LlmClient} based on the configured provider.
 *
 * <p>Spring automatically injects <em>all</em> {@code LlmClient} beans registered in the
 * application context, which are indexed into an in-memory registry keyed by
 * {@link LlmClient#getProviderName()}. At request time, {@link #getClient()} looks up the
 * provider key read from {@code app.llm.provider}.
 *
 * <p>To register a new provider, simply create a Spring {@code @Component} implementing
 * {@link LlmClient} — no changes to this factory are required.
 */
@Slf4j
@Component
public class LlmClientFactory {

    /** Registry of all available providers: providerName → LlmClient. */
    private final Map<String, LlmClient> registry;

    private final LlmProperties properties;

    /**
     * Constructs the factory by auto-discovering all registered {@link LlmClient} beans.
     *
     * @param clients    all LlmClient beans wired by Spring
     * @param properties configuration properties containing the active provider key
     */
    public LlmClientFactory(List<LlmClient> clients, LlmProperties properties) {
        this.properties = properties;
        this.registry = clients.stream()
                .collect(Collectors.toMap(LlmClient::getProviderName, Function.identity()));
        log.info("LlmClientFactory initialised with providers: {}", registry.keySet());
    }

    /**
     * Returns the {@link LlmClient} implementation for the currently configured provider.
     *
     * @return the active LlmClient
     * @throws UnsupportedLlmProviderException if {@code app.llm.provider} does not match
     *         any registered provider
     */
    public LlmClient getClient() {
        String providerKey = properties.getProvider();
        LlmClient client = registry.get(providerKey);

        if (client == null) {
            log.error("No LlmClient registered for provider '{}'. Registered providers: {}",
                    providerKey, registry.keySet());
            throw new UnsupportedLlmProviderException(providerKey, registry.keySet());
        }

        log.debug("Resolved LlmClient for provider '{}'", providerKey);
        return client;
    }

    /** Returns an unmodifiable view of all registered provider names (useful for health checks). */
    public java.util.Set<String> getRegisteredProviders() {
        return java.util.Collections.unmodifiableSet(registry.keySet());
    }
}

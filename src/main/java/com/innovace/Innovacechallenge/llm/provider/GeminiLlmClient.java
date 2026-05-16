package com.innovace.Innovacechallenge.llm.provider;

import com.innovace.Innovacechallenge.exception.LlmCallException;
import com.innovace.Innovacechallenge.llm.LlmClient;
import com.innovace.Innovacechallenge.llm.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * {@link LlmClient} implementation backed by Google Gemini via Spring AI.
 *
 * <p>Uses the Spring AI {@code ChatClient} fluent API to send structured prompts.
 * Registered under the provider key {@value #PROVIDER_NAME}, matching
 * {@link LlmProvider#GEMINI}.
 *
 * <p>Configuration in {@code application.properties}:
 * <pre>
 * app.llm.provider=gemini
 * spring.ai.google.genai.api-key=YOUR_KEY
 * spring.ai.google.genai.chat.options.model=gemini-2.0-flash
 * spring.ai.google.genai.chat.options.temperature=0.2
 * spring.ai.google.genai.chat.options.response-mime-type=application/json
 * </pre>
 */
@Slf4j
@Component
public class GeminiLlmClient implements LlmClient {

    static final String PROVIDER_NAME = LlmProvider.GEMINI.getKey();

    private final ChatClient chatClient;

    /**
     * Builds the Gemini {@link ChatClient} using Spring AI's auto-configured builder.
     *
     * @param builder Spring AI-provided builder pre-configured via application properties
     */
    public GeminiLlmClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        log.info("GeminiLlmClient initialised");
    }

    /**
     * {@inheritDoc}
     *
     * @throws LlmCallException if the Gemini API call fails for any reason
     */
    @Override
    public String call(String systemPrompt, String userMessage) {
        log.debug("Calling Gemini API — user message length: {} chars", userMessage.length());
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            log.debug("Gemini API responded — response length: {} chars",
                    response == null ? 0 : response.length());
            return response;
        } catch (Exception ex) {
            log.error("Gemini API call failed: {}", ex.getMessage(), ex);
            throw new LlmCallException("Gemini API call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}

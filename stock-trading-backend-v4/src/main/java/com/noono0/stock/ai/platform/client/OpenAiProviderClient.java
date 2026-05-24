package com.noono0.stock.ai.platform.client;

import com.noono0.stock.ai.platform.enums.AiProviderType;
import com.noono0.stock.llm.client.OpenAiChatClient;
import com.noono0.stock.llm.config.LlmOpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OpenAiProviderClient implements AiProviderClient {
    private final OpenAiChatClient openAiChatClient;
    private final LlmOpenAiProperties openAiProperties;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.OPENAI;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(openAiProperties.getApiKey());
    }

    @Override
    public AiChatResponse chat(
            String model, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        long t0 = System.currentTimeMillis();
        try {
            String content = openAiChatClient.chatCompletions(model, systemPrompt, userPrompt, temperature);
            return AiChatResponse.ok(content, 0, 0, System.currentTimeMillis() - t0);
        } catch (Exception exception) {
            return AiChatResponse.fail(exception.getMessage(), System.currentTimeMillis() - t0);
        }
    }
}

package com.noono0.stock.ai.platform.client;

import com.noono0.stock.ai.platform.enums.AiProviderType;

public interface AiProviderClient {
    AiProviderType providerType();

    boolean isConfigured();

    AiChatResponse chat(String model, String systemPrompt, String userPrompt, double temperature, int maxTokens);
}

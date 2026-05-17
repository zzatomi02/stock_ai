package com.noono0.stock.ai.platform.client;

import com.noono0.stock.ai.platform.enums.AiProviderType;
import org.springframework.util.StringUtils;

/** Gemini/Claude/Perplexity — API 키 연동 전 스텁 */
public class StubAiProviderClient implements AiProviderClient {
    private final AiProviderType type;
    private final String envName;

    public StubAiProviderClient(AiProviderType type, String envName) {
        this.type = type;
        this.envName = envName;
    }

    @Override
    public AiProviderType providerType() {
        return type;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(System.getenv(envName));
    }

    @Override
    public AiChatResponse chat(
            String model, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        long ms = 1;
        if (!isConfigured()) {
            return AiChatResponse.fail(type + " API 키 미설정 (" + envName + ")", ms);
        }
        return AiChatResponse.fail(type + " 클라이언트 미구현 — REST 연동 예정", ms);
    }
}

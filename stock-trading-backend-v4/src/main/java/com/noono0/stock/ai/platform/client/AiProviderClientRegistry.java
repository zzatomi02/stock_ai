package com.noono0.stock.ai.platform.client;

import com.noono0.stock.ai.platform.enums.AiProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AiProviderClientRegistry {
    private final Map<AiProviderType, AiProviderClient> clients = new EnumMap<>(AiProviderType.class);

    public AiProviderClientRegistry(OpenAiProviderClient openAi) {
        clients.put(AiProviderType.OPENAI, openAi);
        clients.put(AiProviderType.GEMINI, new StubAiProviderClient(AiProviderType.GEMINI, "GEMINI_API_KEY"));
        clients.put(AiProviderType.CLAUDE, new StubAiProviderClient(AiProviderType.CLAUDE, "CLAUDE_API_KEY"));
        clients.put(
                AiProviderType.PERPLEXITY,
                new StubAiProviderClient(AiProviderType.PERPLEXITY, "PERPLEXITY_API_KEY"));
        clients.put(AiProviderType.LOCAL_LLM, new StubAiProviderClient(AiProviderType.LOCAL_LLM, ""));
    }

    public Optional<AiProviderClient> get(String providerType) {
        try {
            return Optional.ofNullable(clients.get(AiProviderType.valueOf(providerType)));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }
}

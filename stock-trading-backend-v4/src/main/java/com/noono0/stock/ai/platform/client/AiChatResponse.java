package com.noono0.stock.ai.platform.client;

public record AiChatResponse(
        String content, int inputTokens, int outputTokens, long elapsedMs, boolean success, String error) {

    public static AiChatResponse ok(String content, int in, int out, long ms) {
        return new AiChatResponse(content, in, out, ms, true, null);
    }

    public static AiChatResponse fail(String error, long ms) {
        return new AiChatResponse(null, 0, 0, ms, false, error);
    }
}

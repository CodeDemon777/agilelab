package com.agilelab.chatapp.service.ai;

public interface AiClient {
    String generate(String prompt, String systemPrompt);
    boolean isAvailable();
    String getProviderName();
}


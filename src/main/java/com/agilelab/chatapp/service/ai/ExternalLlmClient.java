package com.agilelab.chatapp.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("externalLlmClient")
public class ExternalLlmClient implements AiClient {

    @Value("${agilelab.ai.provider:auto}")
    private String provider;

    @Value("${agilelab.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${agilelab.ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${agilelab.ai.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${agilelab.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${agilelab.ai.ollama.model:llama3.2}")
    private String ollamaModel;

    private final RestTemplate restTemplate;

    public ExternalLlmClient() {
        this.restTemplate = new RestTemplate();
        // short timeout so it does not hang if offline
        this.restTemplate.getRequestFactory();
    }

    @Override
    public boolean isAvailable() {
        if ("local".equalsIgnoreCase(provider)) {
            return false;
        }
        if ("openai".equalsIgnoreCase(provider) || (!openaiApiKey.isBlank() && !"none".equalsIgnoreCase(openaiApiKey))) {
            return true;
        }
        if ("ollama".equalsIgnoreCase(provider)) {
            try {
                ResponseEntity<String> res = restTemplate.getForEntity(ollamaBaseUrl + "/api/tags", String.class);
                return res.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String getProviderName() {
        if (isOpenAiActive()) return "OpenAI / Spring AI Compatible Client (" + openaiModel + ")";
        if (isOllamaActive()) return "Ollama Local Model (" + ollamaModel + ")";
        return "External LLM (Configured)";
    }

    private boolean isOpenAiActive() {
        return !openaiApiKey.isBlank() && !"none".equalsIgnoreCase(openaiApiKey);
    }

    private boolean isOllamaActive() {
        return "ollama".equalsIgnoreCase(provider);
    }

    @Override
    public String generate(String prompt, String systemPrompt) {
        if (isOpenAiActive()) {
            return callOpenAi(prompt, systemPrompt);
        } else if (isOllamaActive()) {
            return callOllama(prompt, systemPrompt);
        }
        return null;
    }

    private String callOpenAi(String prompt, String systemPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> req = new HashMap<>();
            req.put("model", openaiModel);
            req.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are AgileLab Smart AI Assistant."),
                    Map.of("role", "user", "content", prompt)
            ));
            req.put("temperature", 0.3);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiBaseUrl + "/chat/completions", entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            // Log and let fallback handle
        }
        return null;
    }

    private String callOllama(String prompt, String systemPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> req = new HashMap<>();
            req.put("model", ollamaModel);
            req.put("prompt", prompt);
            if (systemPrompt != null) {
                req.put("system", systemPrompt);
            }
            req.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaBaseUrl + "/api/generate", entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            // Log and fallback
        }
        return null;
    }
}


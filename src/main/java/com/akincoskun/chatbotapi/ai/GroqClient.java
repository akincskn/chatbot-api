package com.akincoskun.chatbotapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Groq API istemcisi. OpenAI-uyumlu format kullanır.
 * Model: llama-3.3-70b-versatile
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqClient {

    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key}")
    private String apiKey;

    @Value("${app.groq.base-url}")
    private String baseUrl;

    /**
     * Mesaj listesiyle Groq'u çağırır ve yanıtı döner.
     *
     * @param messages system/user/assistant mesajları
     * @return asistan yanıtı
     * @throws RuntimeException API hatası durumunda
     */
    public String chat(List<LlmMessage> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> msgPayload = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", msgPayload,
                "temperature", TEMPERATURE,
                "max_tokens", MAX_TOKENS,
                "stream", false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + COMPLETIONS_PATH, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Groq API error: " + response.getStatusCode());
        }

        return extractContent(response.getBody());
    }

    private String extractContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response", e);
        }
    }
}

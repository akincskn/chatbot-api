package com.akincoskun.chatbotapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini API istemcisi. Groq fallback olarak kullanılır.
 * Model: gemini-2.0-flash
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private static final String GENERATE_PATH = "/models/gemini-2.0-flash:generateContent";
    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    /**
     * Mesaj listesiyle Gemini'yi çağırır ve yanıtı döner.
     * Gemini "system" role'ü desteklemez; system mesajı systemInstruction olarak ayrılır.
     *
     * @param messages system/user/assistant mesajları
     * @return asistan yanıtı
     * @throws RuntimeException API hatası durumunda
     */
    public String chat(List<LlmMessage> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemText = messages.stream()
                .filter(m -> "system".equals(m.role()))
                .map(LlmMessage::content)
                .findFirst().orElse("");

        List<Map<String, Object>> contents = new ArrayList<>();
        for (LlmMessage msg : messages) {
            if ("system".equals(msg.role())) continue;
            String geminiRole = "assistant".equals(msg.role()) ? "model" : "user";
            contents.add(Map.of(
                    "role", geminiRole,
                    "parts", List.of(Map.of("text", msg.content()))
            ));
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemText))),
                "contents", contents,
                "generationConfig", Map.of("maxOutputTokens", MAX_TOKENS, "temperature", TEMPERATURE)
        );

        String url = baseUrl + GENERATE_PATH + "?key=" + apiKey;
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API error: " + response.getStatusCode());
        }

        return extractContent(response.getBody());
    }

    private String extractContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}

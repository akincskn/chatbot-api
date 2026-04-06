package com.akincoskun.chatbotapi.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM çağrılarını yönetir. Groq primary, Gemini fallback.
 * Groq başarısız olursa otomatik olarak Gemini'ye geçer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final GroqClient groqClient;
    private final GeminiClient geminiClient;

    /**
     * Mesaj listesiyle LLM'i çağırır. Groq → Gemini fallback sırası.
     *
     * @param messages system/user/assistant mesajları
     * @return asistan yanıtı
     * @throws RuntimeException her iki LLM de başarısız olursa
     */
    public String chat(List<LlmMessage> messages) {
        try {
            String response = groqClient.chat(messages);
            log.debug("Groq responded successfully");
            return response;
        } catch (Exception groqError) {
            log.warn("Groq failed, falling back to Gemini: {}", groqError.getMessage());
            try {
                String response = geminiClient.chat(messages);
                log.debug("Gemini fallback responded successfully");
                return response;
            } catch (Exception geminiError) {
                log.error("Both LLMs failed. Groq: {}. Gemini: {}", groqError.getMessage(), geminiError.getMessage());
                throw new RuntimeException("All LLM providers failed. Please try again later.");
            }
        }
    }
}

package com.akincoskun.chatbotapi.mapper;

import com.akincoskun.chatbotapi.dto.response.ChatbotResponse;
import com.akincoskun.chatbotapi.entity.Chatbot;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chatbot entity ↔ DTO dönüşümlerini yönetir.
 */
@Component
public class ChatbotMapper {

    /**
     * @param chatbot entity
     * @return yanıt DTO'su
     */
    public ChatbotResponse toResponse(Chatbot chatbot) {
        return new ChatbotResponse(
                chatbot.getId(),
                chatbot.getName(),
                chatbot.getWelcomeMessage(),
                chatbot.getSystemPrompt(),
                chatbot.getPersona(),
                chatbot.getPrimaryColor(),
                chatbot.getStatus(),
                chatbot.getTotalConversations(),
                chatbot.getTotalMessages(),
                chatbot.getCreatedAt(),
                chatbot.getUpdatedAt()
        );
    }

    /**
     * @param chatbots entity listesi
     * @return yanıt DTO listesi
     */
    public List<ChatbotResponse> toResponseList(List<Chatbot> chatbots) {
        return chatbots.stream().map(this::toResponse).toList();
    }
}

package com.akincoskun.chatbotapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Konuşma yanıt DTO'su.
 *
 * @param id            konuşma UUID
 * @param sessionId     ziyaretçi session ID
 * @param createdAt     başlangıç tarihi
 * @param lastMessageAt son mesaj tarihi
 * @param messages      mesaj listesi (opsiyonel — liste endpoint'inde null)
 */
public record ConversationResponse(
        UUID id,
        String sessionId,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt,
        List<MessageResponse> messages
) {
    /**
     * Mesaj yanıt DTO'su (ConversationResponse içinde kullanılır).
     *
     * @param id        mesaj UUID
     * @param role      "user" | "assistant"
     * @param content   mesaj metni
     * @param sources   kaynak chunk'lar (assistant mesajı için)
     * @param createdAt mesaj tarihi
     */
    public record MessageResponse(
            UUID id,
            String role,
            String content,
            List<SourceChunkResponse> sources,
            LocalDateTime createdAt
    ) {}
}

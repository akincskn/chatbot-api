package com.akincoskun.chatbotapi.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Chat mesaj yanıtı.
 *
 * @param message        AI asistanın yanıtı
 * @param conversationId konuşma UUID (client'ın saklayacağı ID)
 * @param sessionId      ziyaretçi session ID
 * @param sources        AI'ın kullandığı kaynak chunk'lar
 */
public record ChatMessageResponse(
        String message,
        UUID conversationId,
        String sessionId,
        List<SourceChunkResponse> sources
) {}

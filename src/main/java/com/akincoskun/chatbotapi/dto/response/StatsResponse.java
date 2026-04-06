package com.akincoskun.chatbotapi.dto.response;

import java.util.UUID;

/**
 * Chatbot istatistik yanıtı.
 *
 * @param chatbotId          chatbot UUID
 * @param name               chatbot adı
 * @param totalConversations toplam konuşma sayısı (gerçek zamanlı)
 * @param totalMessages      toplam mesaj sayısı (denormalized)
 * @param documentCount      yüklü döküman sayısı
 * @param readyDocumentCount işlenmiş döküman sayısı (status=ready)
 */
public record StatsResponse(
        UUID chatbotId,
        String name,
        long totalConversations,
        int totalMessages,
        long documentCount,
        long readyDocumentCount
) {}

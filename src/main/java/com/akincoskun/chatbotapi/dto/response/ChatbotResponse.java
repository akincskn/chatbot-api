package com.akincoskun.chatbotapi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chatbot yanıt DTO'su. Entity'nin tüm public alanlarını içerir.
 *
 * @param id                 chatbot UUID
 * @param name               chatbot adı
 * @param welcomeMessage     karşılama mesajı
 * @param systemPrompt       AI sistem talimatı
 * @param persona            ton (professional/friendly/casual)
 * @param primaryColor       widget rengi (#RRGGBB)
 * @param status             active | inactive
 * @param totalConversations toplam konuşma sayısı
 * @param totalMessages      toplam mesaj sayısı
 * @param createdAt          oluşturulma tarihi
 * @param updatedAt          son güncelleme tarihi
 */
public record ChatbotResponse(
        UUID id,
        String name,
        String welcomeMessage,
        String systemPrompt,
        String persona,
        String primaryColor,
        String status,
        Integer totalConversations,
        Integer totalMessages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

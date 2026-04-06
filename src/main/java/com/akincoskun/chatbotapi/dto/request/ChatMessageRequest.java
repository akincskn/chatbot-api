package com.akincoskun.chatbotapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat mesaj isteği.
 *
 * @param message   kullanıcının sorusu/mesajı (zorunlu)
 * @param sessionId ziyaretçi session ID — public endpoint için zorunlu,
 *                  JWT endpoint için opsiyonel (yoksa yeni konuşma oluşturulur)
 */
public record ChatMessageRequest(

        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message is too long (max 4000 characters)")
        String message,

        @Size(max = 255, message = "Session ID is too long")
        String sessionId
) {}

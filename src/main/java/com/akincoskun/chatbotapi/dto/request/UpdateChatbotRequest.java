package com.akincoskun.chatbotapi.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Chatbot güncelleme isteği. Tüm alanlar opsiyoneldir; null gönderilen alan değişmez.
 *
 * @param name           yeni chatbot adı
 * @param welcomeMessage yeni karşılama mesajı
 * @param systemPrompt   yeni sistem talimatı
 * @param persona        ton: professional | friendly | casual
 * @param primaryColor   widget rengi (#RRGGBB)
 * @param status         active | inactive
 */
public record UpdateChatbotRequest(

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 1000, message = "Welcome message is too long")
        String welcomeMessage,

        @Size(max = 2000, message = "System prompt is too long")
        String systemPrompt,

        @Pattern(
                regexp = "professional|friendly|casual",
                message = "Persona must be one of: professional, friendly, casual"
        )
        String persona,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Primary color must be a valid hex color (e.g. #3B82F6)"
        )
        String primaryColor,

        @Pattern(
                regexp = "active|inactive",
                message = "Status must be one of: active, inactive"
        )
        String status
) {}

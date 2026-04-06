package com.akincoskun.chatbotapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yeni chatbot oluşturma isteği.
 *
 * @param name           chatbot adı (zorunlu)
 * @param welcomeMessage karşılama mesajı (opsiyonel)
 * @param systemPrompt   AI sistem talimatı (opsiyonel)
 * @param persona        ton: professional | friendly | casual (opsiyonel)
 * @param primaryColor   widget rengi (#RRGGBB) (opsiyonel)
 */
public record CreateChatbotRequest(

        @NotBlank(message = "Chatbot name is required")
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
        String primaryColor
) {}

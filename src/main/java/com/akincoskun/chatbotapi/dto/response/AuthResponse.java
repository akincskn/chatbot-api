package com.akincoskun.chatbotapi.dto.response;

import java.util.UUID;

/**
 * Auth işlemi yanıtı. Token ve temel kullanıcı bilgisi döner.
 *
 * @param token   JWT Bearer token
 * @param userId  kullanıcı UUID'si
 * @param email   e-posta
 * @param name    görünen ad
 * @param credits kalan chatbot kredisi
 */
public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String name,
        Integer credits
) {}

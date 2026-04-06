package com.akincoskun.chatbotapi.controller;

import com.akincoskun.chatbotapi.dto.request.ChatMessageRequest;
import com.akincoskun.chatbotapi.dto.response.ChatMessageResponse;
import com.akincoskun.chatbotapi.dto.response.ConversationResponse;
import com.akincoskun.chatbotapi.service.ChatService;
import com.akincoskun.chatbotapi.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Chat endpoint'leri.
 * JWT endpoint'leri chatbot sahibi için test amaçlıdır.
 * Public endpoint'ler embed widget'tan gelen anonim kullanıcılar içindir.
 */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RateLimiterService rateLimiter;

    /**
     * Chatbot sahibinin test chat'i (JWT zorunlu).
     *
     * @param chatbotId   chatbot UUID
     * @param request     mesaj + opsiyonel sessionId
     * @param userDetails JWT kimliği
     * @param httpRequest IP adresi için
     * @return 200 + AI yanıtı ve kaynaklar
     */
    @PostMapping("/api/chat/{chatbotId}")
    public ResponseEntity<ChatMessageResponse> chat(
            @PathVariable UUID chatbotId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        rateLimiter.checkAuthenticated(userDetails.getUsername());
        ChatMessageResponse response = chatService.processOwnerMessage(
                chatbotId,
                userDetails.getUsername(),
                request.message(),
                request.sessionId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Embed widget chat'i (JWT gerektirmez — public).
     *
     * @param chatbotId   chatbot UUID
     * @param request     mesaj + sessionId (localStorage'dan)
     * @param httpRequest IP adresi için
     * @return 200 + AI yanıtı ve kaynaklar
     */
    @PostMapping("/api/chat/public/{chatbotId}")
    public ResponseEntity<ChatMessageResponse> publicChat(
            @PathVariable UUID chatbotId,
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest httpRequest) {

        rateLimiter.checkPublic(httpRequest.getRemoteAddr());
        ChatMessageResponse response = chatService.processPublicMessage(
                chatbotId,
                request.message(),
                request.sessionId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Chatbot'un tüm konuşmalarını listeler (JWT zorunlu).
     *
     * @param chatbotId   chatbot UUID
     * @param userDetails JWT kimliği
     * @return 200 + konuşma listesi
     */
    @GetMapping("/api/chat/{chatbotId}/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @PathVariable UUID chatbotId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(chatService.getConversations(chatbotId, userDetails.getUsername()));
    }

    /**
     * Konuşma detayını mesajlarıyla birlikte getirir (JWT zorunlu).
     *
     * @param conversationId konuşma UUID
     * @param userDetails    JWT kimliği
     * @return 200 + konuşma + mesajlar
     */
    @GetMapping("/api/chat/conversation/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(chatService.getConversation(conversationId, userDetails.getUsername()));
    }
}

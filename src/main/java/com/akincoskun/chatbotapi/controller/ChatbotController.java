package com.akincoskun.chatbotapi.controller;

import com.akincoskun.chatbotapi.dto.request.CreateChatbotRequest;
import com.akincoskun.chatbotapi.dto.request.UpdateChatbotRequest;
import com.akincoskun.chatbotapi.dto.response.ChatbotResponse;
import com.akincoskun.chatbotapi.dto.response.StatsResponse;
import com.akincoskun.chatbotapi.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Chatbot CRUD endpoint'leri. Tüm endpoint'ler JWT gerektirir.
 */
@RestController
@RequestMapping("/api/chatbots")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Kullanıcıya ait tüm chatbot'ları listeler.
     *
     * @param userDetails JWT'den gelen kimlik bilgisi
     * @return 200 + chatbot listesi
     */
    @GetMapping
    public ResponseEntity<List<ChatbotResponse>> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatbotService.getAll(userDetails.getUsername()));
    }

    /**
     * Chatbot detayını getirir.
     *
     * @param id          chatbot UUID
     * @param userDetails JWT kimliği
     * @return 200 + ChatbotResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatbotResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatbotService.getById(id, userDetails.getUsername()));
    }

    /**
     * Yeni chatbot oluşturur. 1 kredi düşürür.
     *
     * @param request     chatbot bilgileri
     * @param userDetails JWT kimliği
     * @return 201 + ChatbotResponse
     */
    @PostMapping
    public ResponseEntity<ChatbotResponse> create(
            @Valid @RequestBody CreateChatbotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatbotResponse response = chatbotService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Chatbot'u günceller. Null alanlar değişmez.
     *
     * @param id          chatbot UUID
     * @param request     güncellenecek alanlar
     * @param userDetails JWT kimliği
     * @return 200 + güncellenmiş ChatbotResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChatbotResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChatbotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatbotService.update(id, request, userDetails.getUsername()));
    }

    /**
     * Chatbot istatistiklerini döner (conversation, mesaj, döküman sayıları).
     *
     * @param id          chatbot UUID
     * @param userDetails JWT kimliği
     * @return 200 + StatsResponse
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<StatsResponse> getStats(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatbotService.getStats(id, userDetails.getUsername()));
    }

    /**
     * Chatbot'u ve tüm ilişkili verileri siler.
     *
     * @param id          chatbot UUID
     * @param userDetails JWT kimliği
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatbotService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

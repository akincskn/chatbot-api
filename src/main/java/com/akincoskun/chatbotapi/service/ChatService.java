package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.dto.response.ChatMessageResponse;
import com.akincoskun.chatbotapi.dto.response.ConversationResponse;
import com.akincoskun.chatbotapi.dto.response.SourceChunkResponse;
import com.akincoskun.chatbotapi.entity.*;
import com.akincoskun.chatbotapi.exception.ResourceNotFoundException;
import com.akincoskun.chatbotapi.exception.UnauthorizedException;
import com.akincoskun.chatbotapi.mapper.ConversationMapper;
import com.akincoskun.chatbotapi.rag.RagPipeline;
import com.akincoskun.chatbotapi.rag.SimilarChunkResult;
import com.akincoskun.chatbotapi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chat mesaj işleme. Konuşma yönetimi, RAG pipeline ve mesaj kayıt işlemlerini koordine eder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatbotRepository chatbotRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RagPipeline ragPipeline;
    private final ConversationMapper conversationMapper;
    private final ObjectMapper objectMapper;

    /**
     * Kullanıcı mesajını işler (JWT korumalı — chatbot sahibi test eder).
     *
     * @param chatbotId   chatbot UUID
     * @param userEmail   JWT'den gelen e-posta
     * @param message     kullanıcının sorusu
     * @param sessionId   konuşma session ID (null ise UUID üretilir)
     * @param visitorIp   IP adresi (opsiyonel)
     * @return AI yanıtı + kaynaklar
     */
    @Transactional
    public ChatMessageResponse processOwnerMessage(UUID chatbotId, String userEmail,
                                                    String message, String sessionId, String visitorIp) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Chatbot chatbot = chatbotRepository.findByIdAndUserId(chatbotId, user.getId())
                .orElseThrow(() -> {
                    if (!chatbotRepository.existsById(chatbotId))
                        return new ResourceNotFoundException("Chatbot not found: " + chatbotId);
                    return new UnauthorizedException("Access denied to chatbot: " + chatbotId);
                });
        return processMessage(chatbot, message, resolveSession(sessionId), visitorIp);
    }

    /**
     * Kullanıcı mesajını işler (JWT gerektirmez — embed widget için).
     *
     * @param chatbotId chatbot UUID
     * @param message   kullanıcının sorusu
     * @param sessionId localStorage'dan gelen session ID
     * @param visitorIp IP adresi
     * @return AI yanıtı + kaynaklar
     */
    @Transactional
    public ChatMessageResponse processPublicMessage(UUID chatbotId, String message,
                                                     String sessionId, String visitorIp) {
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
                .orElseThrow(() -> new ResourceNotFoundException("Chatbot not found: " + chatbotId));
        if (!"active".equals(chatbot.getStatus())) {
            throw new IllegalStateException("This chatbot is currently inactive");
        }
        return processMessage(chatbot, message, resolveSession(sessionId), visitorIp);
    }

    /**
     * Chatbot'un konuşma listesini döner (JWT korumalı).
     *
     * @param chatbotId chatbot UUID
     * @param userEmail sahibin e-postası
     * @return konuşma listesi
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID chatbotId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!chatbotRepository.existsByIdAndUserId(chatbotId, user.getId())) {
            throw new UnauthorizedException("Access denied to chatbot: " + chatbotId);
        }
        return conversationRepository.findByChatbotIdOrderByLastMessageAtDesc(chatbotId)
                .stream().map(conversationMapper::toResponse).toList();
    }

    /**
     * Konuşma detayını mesajlarıyla birlikte döner.
     *
     * @param conversationId konuşma UUID
     * @param userEmail      sahibin e-postası
     * @return konuşma + mesajlar
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, String userEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!chatbotRepository.existsByIdAndUserId(conversation.getChatbot().getId(), user.getId())) {
            throw new UnauthorizedException("Access denied to conversation");
        }
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return conversationMapper.toDetailResponse(conversation, messages);
    }

    private ChatMessageResponse processMessage(Chatbot chatbot, String userMessage,
                                               String sessionId, String visitorIp) {
        // 1. Konuşmayı bul veya oluştur
        boolean[] isNew = {false};
        Conversation conversation = conversationRepository
                .findByChatbotIdAndSessionId(chatbot.getId(), sessionId)
                .orElseGet(() -> { isNew[0] = true; return createConversation(chatbot, sessionId, visitorIp); });

        // 2. Kullanıcı mesajını kaydet
        saveMessage(conversation, "user", userMessage, null);

        // 3. Geçmiş mesajları getir (son 10 — DESC)
        List<Message> history = messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());

        // 4. RAG pipeline
        RagPipeline.RagResponse ragResponse = ragPipeline.process(chatbot, userMessage, history);

        // 5. Kaynakları SourceChunkResponse'a dönüştür
        List<SourceChunkResponse> sources = ragResponse.sourceChunks().stream()
                .map(c -> new SourceChunkResponse(c.content(), c.filename(), c.similarity()))
                .toList();

        // 6. Asistan mesajını kaydet
        saveMessage(conversation, "assistant", ragResponse.answer(), sources);

        // 7. Konuşma istatistiklerini güncelle
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        updateChatbotStats(chatbot, isNew[0]);

        log.info("Chat processed for chatbot {} session {}", chatbot.getId(), sessionId);
        return new ChatMessageResponse(ragResponse.answer(), conversation.getId(), sessionId, sources);
    }

    private Conversation createConversation(Chatbot chatbot, String sessionId, String visitorIp) {
        Conversation conv = Conversation.builder()
                .chatbot(chatbot).sessionId(sessionId).visitorIp(visitorIp).build();
        return conversationRepository.save(conv);
    }

    private void saveMessage(Conversation conv, String role, String content,
                             List<SourceChunkResponse> sources) {
        String sourcesJson = null;
        if (sources != null && !sources.isEmpty()) {
            try { sourcesJson = objectMapper.writeValueAsString(sources); }
            catch (Exception e) { log.warn("Failed to serialize source chunks"); }
        }
        Message msg = Message.builder()
                .conversation(conv).role(role).content(content)
                .sourceChunksJson(sourcesJson)
                .tokenCount(content.length() / 4)
                .build();
        messageRepository.save(msg);
    }

    private void updateChatbotStats(Chatbot chatbot, boolean newConversation) {
        chatbot.setTotalMessages(chatbot.getTotalMessages() + 2);
        if (newConversation) {
            chatbot.setTotalConversations(chatbot.getTotalConversations() + 1);
        }
        chatbotRepository.save(chatbot);
    }

    private String resolveSession(String sessionId) {
        return (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
    }
}

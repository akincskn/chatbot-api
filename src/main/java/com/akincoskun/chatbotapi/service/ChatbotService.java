package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.dto.request.CreateChatbotRequest;
import com.akincoskun.chatbotapi.dto.request.UpdateChatbotRequest;
import com.akincoskun.chatbotapi.dto.response.ChatbotResponse;
import com.akincoskun.chatbotapi.dto.response.StatsResponse;
import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.entity.User;
import com.akincoskun.chatbotapi.exception.CreditExhaustedException;
import com.akincoskun.chatbotapi.exception.ResourceNotFoundException;
import com.akincoskun.chatbotapi.exception.UnauthorizedException;
import com.akincoskun.chatbotapi.mapper.ChatbotMapper;
import com.akincoskun.chatbotapi.repository.ChatbotRepository;
import com.akincoskun.chatbotapi.repository.ConversationRepository;
import com.akincoskun.chatbotapi.repository.DocumentRepository;
import com.akincoskun.chatbotapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Chatbot CRUD işlemleri ve iş kuralları.
 * Ownership kontrolü ve kredi yönetimi bu katmanda yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatbotRepository chatbotRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final DocumentRepository documentRepository;
    private final ChatbotMapper chatbotMapper;

    /**
     * Yeni chatbot oluşturur. Önce kullanıcının kredisi düşürülür (atomik).
     *
     * @param request  chatbot bilgileri
     * @param userEmail işlemi yapan kullanıcının e-postası
     * @return oluşturulan chatbot DTO'su
     * @throws CreditExhaustedException kredi yoksa
     */
    @Transactional
    public ChatbotResponse create(CreateChatbotRequest request, String userEmail) {
        User user = loadUser(userEmail);

        int updated = userRepository.decrementCredits(user.getId());
        if (updated == 0) {
            throw new CreditExhaustedException("You have no chatbot credits left. Please upgrade your plan.");
        }

        Chatbot chatbot = Chatbot.builder()
                .user(user)
                .name(request.name())
                .welcomeMessage(valueOrDefault(request.welcomeMessage(), "Hi! How can I help you today?"))
                .systemPrompt(valueOrDefault(request.systemPrompt(), Chatbot.builder().build().getSystemPrompt()))
                .persona(valueOrDefault(request.persona(), "professional"))
                .primaryColor(valueOrDefault(request.primaryColor(), "#3B82F6"))
                .build();

        Chatbot saved = chatbotRepository.save(chatbot);
        log.info("Chatbot created: {} by user: {}", saved.getId(), userEmail);
        return chatbotMapper.toResponse(saved);
    }

    /**
     * Kullanıcının tüm chatbot'larını döner.
     *
     * @param userEmail kullanıcı e-postası
     * @return chatbot DTO listesi
     */
    @Transactional(readOnly = true)
    public List<ChatbotResponse> getAll(String userEmail) {
        User user = loadUser(userEmail);
        List<Chatbot> chatbots = chatbotRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return chatbotMapper.toResponseList(chatbots);
    }

    /**
     * Chatbot detayını döner. Ownership kontrolü yapılır.
     *
     * @param chatbotId chatbot UUID
     * @param userEmail kullanıcı e-postası
     * @return chatbot DTO
     * @throws ResourceNotFoundException chatbot bulunamazsa
     * @throws UnauthorizedException     sahip değilse
     */
    @Transactional(readOnly = true)
    public ChatbotResponse getById(UUID chatbotId, String userEmail) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);
        return chatbotMapper.toResponse(chatbot);
    }

    /**
     * Chatbot'u günceller. Null gönderilen alanlar değişmez.
     *
     * @param chatbotId chatbot UUID
     * @param request   güncellenecek alanlar
     * @param userEmail kullanıcı e-postası
     * @return güncellenmiş chatbot DTO
     */
    @Transactional
    public ChatbotResponse update(UUID chatbotId, UpdateChatbotRequest request, String userEmail) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);

        if (StringUtils.hasText(request.name()))           chatbot.setName(request.name());
        if (StringUtils.hasText(request.welcomeMessage())) chatbot.setWelcomeMessage(request.welcomeMessage());
        if (StringUtils.hasText(request.systemPrompt()))   chatbot.setSystemPrompt(request.systemPrompt());
        if (StringUtils.hasText(request.persona()))        chatbot.setPersona(request.persona());
        if (StringUtils.hasText(request.primaryColor()))   chatbot.setPrimaryColor(request.primaryColor());
        if (StringUtils.hasText(request.status()))         chatbot.setStatus(request.status());

        Chatbot updated = chatbotRepository.save(chatbot);
        log.info("Chatbot updated: {} by user: {}", chatbotId, userEmail);
        return chatbotMapper.toResponse(updated);
    }

    /**
     * Chatbot'u siler (cascade: tüm dökümanlar, konuşmalar ve mesajlar silinir).
     *
     * @param chatbotId chatbot UUID
     * @param userEmail kullanıcı e-postası
     */
    @Transactional
    public void delete(UUID chatbotId, String userEmail) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);
        chatbotRepository.delete(chatbot);
        log.info("Chatbot deleted: {} by user: {}", chatbotId, userEmail);
    }

    /**
     * Chatbot'un istatistiklerini döner (gerçek zamanlı sayılar).
     *
     * @param chatbotId chatbot UUID
     * @param userEmail sahibin e-postası
     * @return istatistik DTO
     */
    @Transactional(readOnly = true)
    public StatsResponse getStats(UUID chatbotId, String userEmail) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);
        long totalConversations = conversationRepository.countByChatbotId(chatbotId);
        long documentCount = documentRepository.countByChatbotId(chatbotId);
        long readyDocumentCount = documentRepository.countByChatbotIdAndStatus(chatbotId, "ready");
        return new StatsResponse(chatbotId, chatbot.getName(), totalConversations,
                chatbot.getTotalMessages(), documentCount, readyDocumentCount);
    }

    /**
     * Chatbot'un varlığını doğrular. Embed snippet gibi public context'lerde ownership kontrolü gerekmez.
     *
     * @param chatbotId chatbot UUID
     * @throws ResourceNotFoundException chatbot bulunamazsa
     */
    @Transactional(readOnly = true)
    public void assertExists(UUID chatbotId) {
        if (!chatbotRepository.existsById(chatbotId)) {
            throw new ResourceNotFoundException("Chatbot not found: " + chatbotId);
        }
    }

    private Chatbot loadOwnedChatbot(UUID chatbotId, String userEmail) {
        User user = loadUser(userEmail);
        if (!chatbotRepository.existsById(chatbotId)) {
            throw new ResourceNotFoundException("Chatbot not found: " + chatbotId);
        }
        return chatbotRepository.findByIdAndUserId(chatbotId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Access denied to chatbot: " + chatbotId));
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}

package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chatbot veri erişim katmanı.
 */
public interface ChatbotRepository extends JpaRepository<Chatbot, UUID> {

    /**
     * @param userId sahibin UUID'si
     * @return kullanıcıya ait tüm chatbot'lar (yeniden eskiye)
     */
    List<Chatbot> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Chatbot'un belirtilen kullanıcıya ait olup olmadığını doğrular.
     *
     * @param id     chatbot UUID
     * @param userId kullanıcı UUID
     * @return sahibiyse true
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Chatbot'u sahibiyle birlikte getirir (ownership doğrulaması için).
     *
     * @param id     chatbot UUID
     * @param userId kullanıcı UUID
     * @return chatbot varsa Optional içinde
     */
    Optional<Chatbot> findByIdAndUserId(UUID id, UUID userId);
}

package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Konuşma veri erişim katmanı.
 */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Embed widget'tan gelen session_id ile mevcut konuşmayı bulur.
     *
     * @param chatbotId chatbot UUID
     * @param sessionId ziyaretçinin localStorage'daki session ID
     * @return konuşma varsa Optional içinde
     */
    Optional<Conversation> findByChatbotIdAndSessionId(UUID chatbotId, String sessionId);

    /**
     * @param chatbotId chatbot UUID
     * @return konuşmalar (en son aktif önce)
     */
    List<Conversation> findByChatbotIdOrderByLastMessageAtDesc(UUID chatbotId);

    /**
     * @param chatbotId chatbot UUID
     * @return toplam konuşma sayısı
     */
    long countByChatbotId(UUID chatbotId);
}

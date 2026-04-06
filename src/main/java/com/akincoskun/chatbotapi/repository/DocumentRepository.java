package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Döküman veri erişim katmanı.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * @param chatbotId chatbot UUID
     * @return chatbot'a ait dökümanlar (yeniden eskiye)
     */
    List<Document> findByChatbotIdOrderByCreatedAtDesc(UUID chatbotId);

    /**
     * @param id        döküman UUID
     * @param chatbotId chatbot UUID
     * @return döküman varsa Optional içinde
     */
    Optional<Document> findByIdAndChatbotId(UUID id, UUID chatbotId);

    /**
     * @param chatbotId chatbot UUID
     * @return chatbot'a ait döküman sayısı
     */
    long countByChatbotId(UUID chatbotId);

    /**
     * @param chatbotId chatbot UUID
     * @param status    döküman durumu (örn. "ready")
     * @return belirtilen durumdaki döküman sayısı
     */
    long countByChatbotIdAndStatus(UUID chatbotId, String status);
}

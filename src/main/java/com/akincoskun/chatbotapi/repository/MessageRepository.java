package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Mesaj veri erişim katmanı.
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * @param conversationId konuşma UUID
     * @return mesajlar (en eski önce — conversation history için)
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Sliding window history için son N mesajı getirir.
     *
     * @param conversationId konuşma UUID
     * @return son 10 mesaj (en yeni önce; çağıran taraf tersine çevirir)
     */
    List<Message> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}

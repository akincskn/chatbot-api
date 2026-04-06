package com.akincoskun.chatbotapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcıya ait chatbot. Dökümanlar, konuşmalar ve mesajlar bu entity'e bağlıdır.
 */
@Entity
@Table(name = "chatbots", indexes = {
        @Index(name = "idx_chatbots_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    @Builder.Default
    private String welcomeMessage = "Hi! How can I help you today?";

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    @Builder.Default
    private String systemPrompt =
            "You are a helpful assistant. Answer questions based ONLY on the provided context. " +
            "If the answer is not found in the context, politely say you don't have information about that topic.";

    @Column(length = 50)
    @Builder.Default
    private String persona = "professional";

    @Column(name = "primary_color", length = 7)
    @Builder.Default
    private String primaryColor = "#3B82F6";

    @Column(length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "total_conversations", nullable = false)
    @Builder.Default
    private Integer totalConversations = 0;

    @Column(name = "total_messages", nullable = false)
    @Builder.Default
    private Integer totalMessages = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

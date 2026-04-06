package com.akincoskun.chatbotapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bir chatbot oturumu. session_id ile anonim ziyaretçiler takip edilir.
 */
@Entity
@Table(name = "conversations",
        indexes = {
                @Index(name = "idx_conversations_chatbot_id", columnList = "chatbot_id"),
                @Index(name = "idx_conversations_session_id", columnList = "session_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_conversations_chatbot_session",
                        columnNames = {"chatbot_id", "session_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private Chatbot chatbot;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(name = "visitor_ip", length = 45)
    private String visitorIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastMessageAt = LocalDateTime.now();
}

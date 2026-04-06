package com.akincoskun.chatbotapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chatbot'a yüklenen döküman. Kaynak tipi PDF, URL veya düz metin olabilir.
 * İşlem durumu: processing → ready | failed.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_chatbot_id", columnList = "chatbot_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private Chatbot chatbot;

    @Column(nullable = false, length = 500)
    private String filename;

    /** pdf | url | text */
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "content_length")
    @Builder.Default
    private Integer contentLength = 0;

    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;

    /** processing | ready | failed */
    @Column(length = 20)
    @Builder.Default
    private String status = "processing";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

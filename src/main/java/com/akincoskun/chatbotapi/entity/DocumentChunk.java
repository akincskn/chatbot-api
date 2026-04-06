package com.akincoskun.chatbotapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dökümanın bir parçası (chunk). Embedding vektörü JPA dışında native SQL ile
 * yönetilir; sütun VectorSchemaInitializer tarafından oluşturulur.
 *
 * <p>Similarity search için DocumentChunkRepository'deki native sorguya bakın.</p>
 */
@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_chunks_document_id", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "token_count")
    @Builder.Default
    private Integer tokenCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * DocumentChunk veri erişimi. Embedding işlemleri pgvector native SQL ile yapılır.
 */
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    /**
     * Chunk'ın embedding vektörünü günceller.
     * vectorStr formatı: "[0.1,0.2,...,0.384]"
     *
     * @param id        chunk UUID
     * @param vectorStr pgvector string formatında vektör
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE document_chunks SET embedding = CAST(:vectorStr AS vector) WHERE id = CAST(:id AS uuid)",
           nativeQuery = true)
    void updateEmbedding(@Param("id") String id, @Param("vectorStr") String vectorStr);

    /**
     * Bir chatbot'un dokümanlarında semantik benzerlik araması yapar (pgvector cosine distance).
     * Sonuç sütunları: id, content, chunk_index, filename, similarity
     *
     * @param chatbotId   chatbot UUID
     * @param queryVector sorgu vektörü ("[0.1,...,0.384]" formatında)
     * @param limit       max dönen chunk sayısı
     * @param minSim      minimum benzerlik eşiği (0-1)
     * @return [id, content, chunk_index, filename, similarity] satırları
     */
    @Query(value = """
            SELECT dc.id,
                   dc.content,
                   dc.chunk_index,
                   d.filename,
                   1 - (dc.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.chatbot_id = CAST(:chatbotId AS uuid)
              AND d.status = 'ready'
              AND dc.embedding IS NOT NULL
              AND 1 - (dc.embedding <=> CAST(:queryVector AS vector)) > :minSim
            ORDER BY dc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """,
           nativeQuery = true)
    List<Object[]> findSimilarChunks(
            @Param("chatbotId") String chatbotId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("minSim") double minSim);
}

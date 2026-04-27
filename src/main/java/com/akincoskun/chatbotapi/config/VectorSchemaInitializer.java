package com.akincoskun.chatbotapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Uygulama başlarken pgvector extension'ını ve embedding sütununu oluşturur.
 * JPA DDL'den sonra çalışır; bu yüzden ApplicationRunner kullanılır.
 *
 * <p>Neon'da pgvector extension zaten etkin olmalıdır. Bu sınıf sadece
 * document_chunks tablosuna vector(384) sütunu ekler.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector extension verified");
        } catch (Exception e) {
            log.warn("Could not create vector extension (may already exist or require superuser): {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                "ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding vector(384)");
            log.info("document_chunks.embedding column verified");
        } catch (Exception e) {
            log.warn("Could not add embedding column (may already exist): {}", e.getMessage());
        }

        // Drop legacy IVFFlat index — it requires ≥100 rows to train and returns 0 results
        // on small tables where all centroids collapse to zero. HNSW has no such requirement.
        try {
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_chunks_embedding");
            log.info("Dropped legacy IVFFlat index (if it existed)");
        } catch (Exception e) {
            log.debug("Could not drop IVFFlat index: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_chunks_embedding_hnsw " +
                "ON document_chunks USING hnsw (embedding vector_cosine_ops)");
            log.info("HNSW index on embedding verified");
        } catch (Exception e) {
            log.warn("Could not create HNSW index (pgvector ≥0.5.0 required): {}", e.getMessage());
        }
    }
}

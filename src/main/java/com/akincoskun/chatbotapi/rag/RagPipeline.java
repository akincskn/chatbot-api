package com.akincoskun.chatbotapi.rag;

import com.akincoskun.chatbotapi.ai.LlmMessage;
import com.akincoskun.chatbotapi.ai.LlmService;
import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.entity.Message;
import com.akincoskun.chatbotapi.repository.DocumentChunkRepository;
import com.akincoskun.chatbotapi.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG (Retrieval Augmented Generation) pipeline'ı.
 * Sorgu embedding → semantic search → context build → LLM çağrısı → yanıt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagPipeline {

    private static final int TOP_K = 5;
    private static final double MIN_SIMILARITY = 0.1;

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final ContextBuilder contextBuilder;
    private final LlmService llmService;

    /**
     * Kullanıcı sorusunu işler ve yanıt üretir.
     *
     * @param chatbot        chatbot entity
     * @param userQuestion   kullanıcının sorusu
     * @param recentMessages konuşma geçmişi (en yeni önce)
     * @return pipeline yanıtı (LLM yanıtı + kullanılan chunk'lar)
     */
    public RagResponse process(Chatbot chatbot, String userQuestion, List<Message> recentMessages) {
        log.info("RAG pipeline start — chatbotId={}, query=\"{}\"", chatbot.getId(), userQuestion);

        // 1. Sorgu embedding
        float[] queryVector = embeddingService.embed(userQuestion);
        log.info("Query embedding dimension: {}", queryVector.length);
        String queryVectorStr = embeddingService.toVectorString(queryVector);

        // 2. Semantic search
        List<SimilarChunkResult> similarChunks = findSimilarChunks(chatbot.getId().toString(), queryVectorStr);
        log.info("Found {} similar chunks for chatbot {} (minSimilarity={})", similarChunks.size(), chatbot.getId(), MIN_SIMILARITY);
        for (int i = 0; i < similarChunks.size(); i++) {
            SimilarChunkResult c = similarChunks.get(i);
            log.info("  Chunk[{}]: similarity={} file=\"{}\" preview=\"{}\"",
                    i, String.format("%.4f", c.similarity()), c.filename(),
                    c.content().length() > 80 ? c.content().substring(0, 80) + "..." : c.content());
        }

        // 3. Context oluştur
        List<LlmMessage> messages = contextBuilder.build(chatbot, similarChunks, recentMessages, userQuestion);
        int contextLength = messages.stream().mapToInt(m -> m.content().length()).sum();
        log.info("Context length: {} chars across {} messages", contextLength, messages.size());

        // 4. LLM çağrısı (Groq → Gemini fallback)
        String llmResponse = llmService.chat(messages);
        log.info("LLM response length: {} chars", llmResponse.length());

        return new RagResponse(llmResponse, similarChunks);
    }

    private List<SimilarChunkResult> findSimilarChunks(String chatbotId, String queryVectorStr) {
        log.info("Searching similar chunks — chatbotId={} limit={} minSim={}", chatbotId, TOP_K, MIN_SIMILARITY);
        List<Object[]> rows = chunkRepository.findSimilarChunks(chatbotId, queryVectorStr, TOP_K, MIN_SIMILARITY);
        log.info("findSimilarChunks returned {} raw rows", rows.size());

        return rows.stream().map(row -> new SimilarChunkResult(
                row[0] != null ? row[0].toString() : "",
                row[1] != null ? row[1].toString() : "",
                row[2] != null ? ((Number) row[2]).intValue() : 0,
                row[3] != null ? row[3].toString() : "",
                row[4] != null ? sanitizeSimilarity(((Number) row[4]).doubleValue()) : 0.0
        )).toList();
    }

    // Zero-vector or degenerate embedding → cosine distance is undefined (NaN/Infinity).
    // Clamp to [0,1] so downstream JSON serialization always produces a valid number.
    private static double sanitizeSimilarity(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * RAG pipeline yanıtı.
     *
     * @param answer       LLM'den gelen yanıt metni
     * @param sourceChunks kullanılan kaynak chunk'lar
     */
    public record RagResponse(String answer, List<SimilarChunkResult> sourceChunks) {}
}

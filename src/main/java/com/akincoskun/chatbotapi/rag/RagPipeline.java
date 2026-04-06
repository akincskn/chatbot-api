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
    private static final double MIN_SIMILARITY = 0.3;

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
        // 1. Sorgu embedding
        float[] queryVector = embeddingService.embed(userQuestion);
        String queryVectorStr = embeddingService.toVectorString(queryVector);

        // 2. Semantic search
        List<SimilarChunkResult> similarChunks = findSimilarChunks(chatbot.getId().toString(), queryVectorStr);
        log.debug("Found {} similar chunks for chatbot {}", similarChunks.size(), chatbot.getId());

        // 3. Context oluştur
        List<LlmMessage> messages = contextBuilder.build(chatbot, similarChunks, recentMessages, userQuestion);

        // 4. LLM çağrısı (Groq → Gemini fallback)
        String llmResponse = llmService.chat(messages);

        return new RagResponse(llmResponse, similarChunks);
    }

    private List<SimilarChunkResult> findSimilarChunks(String chatbotId, String queryVectorStr) {
        List<Object[]> rows = chunkRepository.findSimilarChunks(chatbotId, queryVectorStr, TOP_K, MIN_SIMILARITY);

        return rows.stream().map(row -> new SimilarChunkResult(
                row[0] != null ? row[0].toString() : "",
                row[1] != null ? row[1].toString() : "",
                row[2] != null ? ((Number) row[2]).intValue() : 0,
                row[3] != null ? row[3].toString() : "",
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0
        )).toList();
    }

    /**
     * RAG pipeline yanıtı.
     *
     * @param answer       LLM'den gelen yanıt metni
     * @param sourceChunks kullanılan kaynak chunk'lar
     */
    public record RagResponse(String answer, List<SimilarChunkResult> sourceChunks) {}
}

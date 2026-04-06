package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.ai.HuggingFaceEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding üretimi için servis katmanı. HuggingFace client'ı sarar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final HuggingFaceEmbeddingClient embeddingClient;

    /**
     * Tek metin için 384 boyutlu embedding üretir.
     *
     * @param text giriş metni
     * @return float[384] vektör
     */
    public float[] embed(String text) {
        log.debug("Generating embedding for text ({} chars)", text.length());
        return embeddingClient.embed(text);
    }

    /**
     * Metin listesi için batch embedding üretir.
     *
     * @param texts giriş metinleri
     * @return embedding listesi (sıralı)
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());
        return embeddingClient.embedBatch(texts);
    }

    /**
     * float[] vektörünü pgvector string formatına dönüştürür.
     *
     * @param vector embedding vektörü
     * @return "[0.1,0.2,...]" formatında string
     */
    public String toVectorString(float[] vector) {
        return embeddingClient.toVectorString(vector);
    }
}

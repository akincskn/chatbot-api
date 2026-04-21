package com.akincoskun.chatbotapi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HuggingFace Inference API'yi kullanarak text embedding üretir.
 * Model: sentence-transformers/all-MiniLM-L6-v2 → 384 boyutlu vektör.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceEmbeddingClient {

    private static final String API_URL =
            "https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/feature-extraction";
    private static final int BATCH_SIZE = 5;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.huggingface.api-key}")
    private String apiKey;

    /**
     * Tek bir metin için embedding üretir.
     *
     * @param text giriş metni
     * @return 384 boyutlu float vektör
     */
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.getFirst();
    }

    /**
     * Metin listesi için batch embedding üretir (5'er grupla API çağrısı yapar).
     *
     * @param texts giriş metinleri
     * @return sırayla embedding listesi
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            List<float[]> batchResult = callApiWithRetry(batch);
            allEmbeddings.addAll(batchResult);
        }

        return allEmbeddings;
    }

    private List<float[]> callApiWithRetry(List<String> texts) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callApi(texts);
            } catch (Exception e) {
                lastException = e;
                log.warn("HuggingFace API attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw new RuntimeException("HuggingFace embedding failed after " + MAX_RETRIES + " attempts", lastException);
    }

    private List<float[]> callApi(List<String> texts) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of("inputs", texts, "options", Map.of("wait_for_model", true));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("HuggingFace API call → URL: {}", API_URL);
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("HuggingFace API error: " + response.getStatusCode());
        }

        return parseEmbeddingResponse(response.getBody(), texts.size());
    }

    private List<float[]> parseEmbeddingResponse(String json, int expectedCount) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<float[]> embeddings = new ArrayList<>();

        for (int i = 0; i < expectedCount; i++) {
            JsonNode vectorNode = root.get(i);
            float[] vector = new float[vectorNode.size()];
            for (int j = 0; j < vectorNode.size(); j++) {
                vector[j] = (float) vectorNode.get(j).asDouble();
            }
            embeddings.add(vector);
        }

        return embeddings;
    }

    /**
     * float[] vektörünü pgvector string formatına dönüştürür: "[0.1,0.2,...]"
     *
     * @param vector float array
     * @return pgvector uyumlu string
     */
    public String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

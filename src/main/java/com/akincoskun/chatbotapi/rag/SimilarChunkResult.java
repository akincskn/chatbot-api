package com.akincoskun.chatbotapi.rag;

/**
 * Semantic similarity arama sonucu.
 *
 * @param chunkId    chunk UUID (string — native query'den gelir)
 * @param content    chunk metni
 * @param chunkIndex döküman içindeki sıra
 * @param filename   kaynak döküman adı
 * @param similarity cosine benzerlik skoru (0-1)
 */
public record SimilarChunkResult(
        String chunkId,
        String content,
        int chunkIndex,
        String filename,
        double similarity
) {}

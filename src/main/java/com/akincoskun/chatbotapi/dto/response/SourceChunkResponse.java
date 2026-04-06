package com.akincoskun.chatbotapi.dto.response;

/**
 * RAG pipeline'ın kullandığı kaynak chunk bilgisi.
 *
 * @param content    chunk metni
 * @param filename   kaynak döküman adı
 * @param similarity cosine benzerlik skoru (0-1)
 */
public record SourceChunkResponse(String content, String filename, double similarity) {}

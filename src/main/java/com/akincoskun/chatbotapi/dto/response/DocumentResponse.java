package com.akincoskun.chatbotapi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Döküman yanıt DTO'su.
 *
 * @param id            döküman UUID
 * @param filename      görünen ad
 * @param sourceType    pdf | url | text
 * @param sourceUrl     URL kaynaklıysa adres
 * @param contentLength kelime sayısı
 * @param chunkCount    oluşturulan chunk sayısı
 * @param status        processing | ready | failed
 * @param errorMessage  hata varsa açıklama
 * @param createdAt     yükleme tarihi
 */
public record DocumentResponse(
        UUID id,
        String filename,
        String sourceType,
        String sourceUrl,
        Integer contentLength,
        Integer chunkCount,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {}

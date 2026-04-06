package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.util.TextCleaner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Metni örtüşen (overlapping) chunk'lara böler.
 * Önce paragraf sınırlarına, sonra cümlelere, sonra kelimelere göre böler.
 */
@Service
public class TextSplitterService {

    private static final int CHUNK_SIZE_TOKENS = 500;
    private static final int OVERLAP_TOKENS = 50;
    private static final int CHUNK_SIZE_CHARS = CHUNK_SIZE_TOKENS * 4;
    private static final int OVERLAP_CHARS = OVERLAP_TOKENS * 4;

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", ". ", " ", "");

    /**
     * Metni örtüşen chunk'lara böler.
     *
     * @param text temizlenmiş kaynak metin
     * @return chunk listesi (boş string içermez)
     */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> chunks = new ArrayList<>();
        splitRecursive(text, SEPARATORS, 0, chunks);
        return chunks.stream().filter(c -> !c.isBlank()).toList();
    }

    private void splitRecursive(String text, List<String> separators, int separatorIndex, List<String> result) {
        if (text.length() <= CHUNK_SIZE_CHARS) {
            result.add(text.strip());
            return;
        }

        if (separatorIndex >= separators.size()) {
            // Separator kalmadı — hard cut
            for (int i = 0; i < text.length(); i += CHUNK_SIZE_CHARS - OVERLAP_CHARS) {
                int end = Math.min(i + CHUNK_SIZE_CHARS, text.length());
                result.add(text.substring(i, end).strip());
            }
            return;
        }

        String separator = separators.get(separatorIndex);
        if (separator.isEmpty()) {
            splitRecursive(text, separators, separatorIndex + 1, result);
            return;
        }

        String[] parts = text.split(java.util.regex.Pattern.quote(separator), -1);

        if (parts.length <= 1) {
            // Bu separator çalışmadı — bir sonrakini dene
            splitRecursive(text, separators, separatorIndex + 1, result);
            return;
        }

        mergeAndSplit(parts, separator, separators, separatorIndex, result);
    }

    private void mergeAndSplit(String[] parts, String separator, List<String> separators,
                                int separatorIndex, List<String> result) {
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (current.length() + separator.length() + part.length() <= CHUNK_SIZE_CHARS) {
                if (!current.isEmpty()) current.append(separator);
                current.append(part);
            } else {
                if (!current.isEmpty()) {
                    String chunk = current.toString().strip();
                    if (chunk.length() > CHUNK_SIZE_CHARS) {
                        splitRecursive(chunk, separators, separatorIndex + 1, result);
                    } else {
                        result.add(chunk);
                    }
                    // Overlap: sonraki chunk'a mevcut chunk'ın sonunu ekle
                    String overlap = extractOverlap(current.toString());
                    current = new StringBuilder(overlap);
                }
                current.append(part);
            }
        }

        if (!current.isEmpty()) {
            String chunk = current.toString().strip();
            if (chunk.length() > CHUNK_SIZE_CHARS) {
                splitRecursive(chunk, separators, separatorIndex + 1, result);
            } else if (!chunk.isBlank()) {
                result.add(chunk);
            }
        }
    }

    private String extractOverlap(String text) {
        if (text.length() <= OVERLAP_CHARS) return text;
        return text.substring(text.length() - OVERLAP_CHARS);
    }

    /**
     * @param text metin
     * @return tahmini token sayısı
     */
    public int estimateTokens(String text) {
        return TextCleaner.estimateTokens(text);
    }
}

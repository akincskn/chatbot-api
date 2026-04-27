package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.entity.Document;
import com.akincoskun.chatbotapi.entity.DocumentChunk;
import com.akincoskun.chatbotapi.exception.ResourceNotFoundException;
import com.akincoskun.chatbotapi.repository.DocumentChunkRepository;
import com.akincoskun.chatbotapi.repository.DocumentRepository;
import com.akincoskun.chatbotapi.util.TextCleaner;
import com.akincoskun.chatbotapi.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Döküman içeriğini asenkron olarak işler (parse → split → embed → save).
 * Ayrı bean olduğu için @Async Spring proxy üzerinden çalışır (self-invocation bypass yok).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessor {

    private static final int URL_TIMEOUT_MS = 10_000;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TextSplitterService textSplitter;
    private final EmbeddingService embeddingService;

    /**
     * URL veya text dökümanını arka planda işler.
     *
     * @param docId      document UUID
     * @param sourceType "url" | "text"
     * @param url        kaynak URL (sourceType="url" ise)
     * @param rawText    düz metin (sourceType="text" ise)
     */
    @Async
    @Transactional
    public void processAsync(UUID docId, String sourceType, String url, String rawText) {
        try {
            String content = switch (sourceType) {
                case "url" -> fetchUrl(url);
                case "text" -> rawText;
                default -> throw new IllegalArgumentException("Unknown sourceType: " + sourceType);
            };
            processContent(docId, content);
        } catch (Exception e) {
            markFailed(docId, e.getMessage());
        }
    }

    /**
     * PDF dökümanını arka planda işler. Byte array alır çünkü async thread'de
     * MultipartFile input stream request context'i kapalı olabilir.
     *
     * @param docId     document UUID
     * @param pdfBytes  PDF dosyasının byte içeriği
     */
    @Async
    @Transactional
    public void processPdfAsync(UUID docId, byte[] pdfBytes) {
        try {
            String content;
            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                content = new PDFTextStripper().getText(pdf);
            }
            processContent(docId, content);
        } catch (Exception e) {
            markFailed(docId, e.getMessage());
        }
    }

    private void processContent(UUID docId, String rawContent) {
        log.info("Processing document: {}", docId);
        String cleaned = TextCleaner.clean(rawContent);
        List<String> chunks = textSplitter.split(cleaned);
        log.info("Document {} split into {} chunks (content length: {} chars)", docId, chunks.size(), cleaned.length());

        if (chunks.isEmpty()) {
            markFailed(docId, "No content could be extracted from the document");
            return;
        }

        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        doc.setOriginalContent(cleaned.length() > 50000 ? cleaned.substring(0, 50000) : cleaned);
        doc.setContentLength(TextCleaner.estimateTokens(cleaned));
        documentRepository.save(doc);

        saveChunksWithEmbeddings(doc, chunks);

        doc.setChunkCount(chunks.size());
        doc.setStatus("ready");
        documentRepository.save(doc);
        log.info("Document {} processed successfully: {} chunks, status=ready", docId, chunks.size());
    }

    private void saveChunksWithEmbeddings(Document doc, List<String> chunks) {
        log.info("Generating embeddings for {} chunks (documentId={})", chunks.size(), doc.getId());
        List<float[]> embeddings = embeddingService.embedBatch(chunks);
        log.info("Received {} embeddings from HuggingFace", embeddings.size());

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(doc)
                    .content(chunks.get(i))
                    .chunkIndex(i)
                    .tokenCount(textSplitter.estimateTokens(chunks.get(i)))
                    .build();

            // saveAndFlush ensures INSERT is visible to the native UPDATE in the same transaction.
            DocumentChunk saved = chunkRepository.saveAndFlush(chunk);
            float[] embedding = embeddings.get(i);
            String vectorStr = embeddingService.toVectorString(embedding);
            chunkRepository.updateEmbedding(saved.getId().toString(), vectorStr);
            log.info("Saved chunk {}/{} with embedding dimension: {} (chunkId={})",
                    i + 1, chunks.size(), embedding.length, saved.getId());
        }
    }

    private String fetchUrl(String url) {
        UrlValidator.validateForFetch(url);
        try {
            return Jsoup.connect(url).timeout(URL_TIMEOUT_MS).get().body().text();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch URL: " + e.getMessage(), e);
        }
    }

    private void markFailed(UUID docId, String error) {
        log.error("Document processing failed {}: {}", docId, error);
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus("failed");
            doc.setErrorMessage(error != null && error.length() > 500 ? error.substring(0, 500) : error);
            documentRepository.save(doc);
        });
    }
}

package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.dto.response.DocumentResponse;
import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.entity.Document;
import com.akincoskun.chatbotapi.entity.DocumentChunk;
import com.akincoskun.chatbotapi.exception.ResourceNotFoundException;
import com.akincoskun.chatbotapi.exception.UnauthorizedException;
import com.akincoskun.chatbotapi.mapper.DocumentMapper;
import com.akincoskun.chatbotapi.repository.ChatbotRepository;
import com.akincoskun.chatbotapi.repository.DocumentChunkRepository;
import com.akincoskun.chatbotapi.repository.DocumentRepository;
import com.akincoskun.chatbotapi.repository.UserRepository;
import com.akincoskun.chatbotapi.util.TextCleaner;
import com.akincoskun.chatbotapi.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Döküman yükleme ve işleme (parse → split → embed → save) operasyonları.
 * İşleme @Async ile arka planda yapılır; upload endpoint hemen döner.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final int URL_TIMEOUT_MS = 10_000;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChatbotRepository chatbotRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;
    private final TextSplitterService textSplitter;
    private final EmbeddingService embeddingService;

    /** URL veya text döküman yükler; işlem arka planda başlar. */
    @Transactional
    public DocumentResponse upload(UUID chatbotId, String userEmail, String sourceType,
                                   String url, String text, String filename) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);

        String resolvedFilename = resolveFilename(filename, sourceType, url);
        Document doc = Document.builder()
                .chatbot(chatbot)
                .filename(resolvedFilename)
                .sourceType(sourceType)
                .sourceUrl("url".equals(sourceType) ? url : null)
                .status("processing")
                .build();

        Document saved = documentRepository.save(doc);
        processAsync(saved.getId(), sourceType, url, text);
        return documentMapper.toResponse(saved);
    }

    /** PDF döküman yükler; işlem arka planda başlar. */
    @Transactional
    public DocumentResponse uploadPdf(UUID chatbotId, String userEmail, MultipartFile file) {
        Chatbot chatbot = loadOwnedChatbot(chatbotId, userEmail);

        Document doc = Document.builder()
                .chatbot(chatbot)
                .filename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf")
                .sourceType("pdf")
                .status("processing")
                .build();

        Document saved = documentRepository.save(doc);
        processPdfAsync(saved.getId(), file);
        return documentMapper.toResponse(saved);
    }

    /** Chatbot'a ait döküman listesini döner. */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll(UUID chatbotId, String userEmail) {
        loadOwnedChatbot(chatbotId, userEmail);
        return documentMapper.toResponseList(documentRepository.findByChatbotIdOrderByCreatedAtDesc(chatbotId));
    }

    /** Dökümanı ve tüm chunk'larını siler. */
    @Transactional
    public void delete(UUID chatbotId, UUID documentId, String userEmail) {
        loadOwnedChatbot(chatbotId, userEmail);
        Document doc = documentRepository.findByIdAndChatbotId(documentId, chatbotId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentRepository.delete(doc);
        log.info("Document deleted: {} from chatbot: {}", documentId, chatbotId);
    }

    @Async
    protected void processAsync(UUID docId, String sourceType, String url, String rawText) {
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

    @Async
    protected void processPdfAsync(UUID docId, MultipartFile file) {
        try {
            String content;
            try (PDDocument pdf = Loader.loadPDF(file.getInputStream().readAllBytes())) {
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

            // saveAndFlush ensures the INSERT is committed to DB before the native UPDATE runs.
            // Without flush, @Modifying UPDATE executes while INSERT is still in JPA cache → 0 rows updated → embedding stays NULL.
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

    private Chatbot loadOwnedChatbot(UUID chatbotId, String userEmail) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        if (!chatbotRepository.existsById(chatbotId)) {
            throw new ResourceNotFoundException("Chatbot not found: " + chatbotId);
        }
        return chatbotRepository.findByIdAndUserId(chatbotId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Access denied to chatbot: " + chatbotId));
    }

    private String resolveFilename(String filename, String sourceType, String url) {
        if (filename != null && !filename.isBlank()) return filename;
        if ("url".equals(sourceType) && url != null) {
            try { return new java.net.URI(url).getHost(); } catch (Exception ignored) {}
        }
        return "untitled";
    }
}

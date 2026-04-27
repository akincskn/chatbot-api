package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.dto.response.DocumentResponse;
import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.entity.Document;
import com.akincoskun.chatbotapi.exception.ResourceNotFoundException;
import com.akincoskun.chatbotapi.exception.UnauthorizedException;
import com.akincoskun.chatbotapi.mapper.DocumentMapper;
import com.akincoskun.chatbotapi.repository.ChatbotRepository;
import com.akincoskun.chatbotapi.repository.DocumentRepository;
import com.akincoskun.chatbotapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Döküman yükleme ve listeleme. İşleme (parse → split → embed → save)
 * DocumentProcessor tarafından asenkron olarak yürütülür.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChatbotRepository chatbotRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;
    private final DocumentProcessor documentProcessor;

    /**
     * URL veya text döküman yükler; işlem arka planda başlar.
     *
     * @param chatbotId  chatbot UUID
     * @param userEmail  sahibin e-postası
     * @param sourceType "url" | "text"
     * @param url        kaynak URL (sourceType="url" ise)
     * @param text       düz metin (sourceType="text" ise)
     * @param filename   görünen dosya adı (opsiyonel)
     * @return döküman DTO (status="processing")
     */
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
        // Separate bean call → Spring proxy applies @Async correctly.
        documentProcessor.processAsync(saved.getId(), sourceType, url, text);
        log.info("Document upload queued: {} (chatbotId={})", saved.getId(), chatbotId);
        return documentMapper.toResponse(saved);
    }

    /**
     * PDF döküman yükler; işlem arka planda başlar.
     *
     * @param chatbotId chatbot UUID
     * @param userEmail sahibin e-postası
     * @param file      yüklenen PDF dosyası
     * @return döküman DTO (status="processing")
     */
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

        // Read bytes synchronously; async thread may run after request context closes.
        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage(), e);
        }

        documentProcessor.processPdfAsync(saved.getId(), pdfBytes);
        log.info("PDF upload queued: {} (chatbotId={})", saved.getId(), chatbotId);
        return documentMapper.toResponse(saved);
    }

    /**
     * Chatbot'a ait döküman listesini döner.
     *
     * @param chatbotId chatbot UUID
     * @param userEmail sahibin e-postası
     * @return döküman listesi
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll(UUID chatbotId, String userEmail) {
        loadOwnedChatbot(chatbotId, userEmail);
        return documentMapper.toResponseList(documentRepository.findByChatbotIdOrderByCreatedAtDesc(chatbotId));
    }

    /**
     * Dökümanı ve tüm chunk'larını siler.
     *
     * @param chatbotId  chatbot UUID
     * @param documentId silinecek döküman UUID
     * @param userEmail  sahibin e-postası
     */
    @Transactional
    public void delete(UUID chatbotId, UUID documentId, String userEmail) {
        loadOwnedChatbot(chatbotId, userEmail);
        Document doc = documentRepository.findByIdAndChatbotId(documentId, chatbotId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentRepository.delete(doc);
        log.info("Document deleted: {} from chatbot: {}", documentId, chatbotId);
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

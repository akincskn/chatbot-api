package com.akincoskun.chatbotapi.controller;

import com.akincoskun.chatbotapi.dto.request.UploadDocumentRequest;
import com.akincoskun.chatbotapi.dto.response.DocumentResponse;
import com.akincoskun.chatbotapi.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Döküman yükleme ve listeleme endpoint'leri.
 * Tüm endpoint'ler JWT gerektirir ve chatbot ownership kontrolü yapılır.
 */
@RestController
@RequestMapping("/api/chatbots/{chatbotId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * URL veya düz metin döküman yükler. İşlem arka planda başlar.
     *
     * @param chatbotId   chatbot UUID
     * @param request     sourceType, url/text içeriği
     * @param userDetails JWT kimliği
     * @return 202 + DocumentResponse (status: processing)
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID chatbotId,
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentResponse response = documentService.upload(
                chatbotId,
                userDetails.getUsername(),
                request.sourceType(),
                request.url(),
                request.text(),
                request.filename()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * PDF dosyası yükler. İşlem arka planda başlar.
     *
     * @param chatbotId   chatbot UUID
     * @param file        PDF dosyası (multipart/form-data)
     * @param userDetails JWT kimliği
     * @return 202 + DocumentResponse (status: processing)
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadPdf(
            @PathVariable UUID chatbotId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentResponse response = documentService.uploadPdf(
                chatbotId,
                userDetails.getUsername(),
                file
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Chatbot'a ait dökümanları listeler.
     *
     * @param chatbotId   chatbot UUID
     * @param userDetails JWT kimliği
     * @return 200 + döküman listesi
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll(
            @PathVariable UUID chatbotId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(documentService.getAll(chatbotId, userDetails.getUsername()));
    }

    /**
     * Dökümanı ve tüm chunk'larını siler.
     *
     * @param chatbotId   chatbot UUID
     * @param documentId  döküman UUID
     * @param userDetails JWT kimliği
     * @return 204 No Content
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID chatbotId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.delete(chatbotId, documentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

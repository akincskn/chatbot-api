package com.akincoskun.chatbotapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * URL veya düz metin döküman yükleme isteği (JSON body).
 * PDF yüklemek için multipart endpoint kullanılır.
 *
 * @param sourceType kaynak tipi: "url" | "text"
 * @param url        URL tipi için kaynak adresi
 * @param text       text tipi için içerik
 * @param filename   görünen ad (opsiyonel; URL için domain, text için "untitled")
 */
public record UploadDocumentRequest(

        @NotBlank(message = "sourceType is required")
        @Pattern(regexp = "url|text", message = "sourceType must be 'url' or 'text'")
        String sourceType,

        @Size(max = 2048, message = "URL is too long")
        String url,

        String text,

        @Size(max = 500, message = "Filename is too long")
        String filename
) {}

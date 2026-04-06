package com.akincoskun.chatbotapi.mapper;

import com.akincoskun.chatbotapi.dto.response.DocumentResponse;
import com.akincoskun.chatbotapi.entity.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Document entity ↔ DTO dönüşümleri.
 */
@Component
public class DocumentMapper {

    /**
     * @param document entity
     * @return yanıt DTO'su
     */
    public DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getSourceType(),
                document.getSourceUrl(),
                document.getContentLength(),
                document.getChunkCount(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getCreatedAt()
        );
    }

    /**
     * @param documents entity listesi
     * @return yanıt DTO listesi
     */
    public List<DocumentResponse> toResponseList(List<Document> documents) {
        return documents.stream().map(this::toResponse).toList();
    }
}

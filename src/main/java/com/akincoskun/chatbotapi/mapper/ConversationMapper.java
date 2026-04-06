package com.akincoskun.chatbotapi.mapper;

import com.akincoskun.chatbotapi.dto.response.ConversationResponse;
import com.akincoskun.chatbotapi.dto.response.SourceChunkResponse;
import com.akincoskun.chatbotapi.entity.Conversation;
import com.akincoskun.chatbotapi.entity.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversation / Message entity ↔ DTO dönüşümleri.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationMapper {

    private final ObjectMapper objectMapper;

    /**
     * Mesajsız konuşma listesi için DTO üretir.
     *
     * @param conversation entity
     * @return ConversationResponse (messages null)
     */
    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getSessionId(),
                conversation.getCreatedAt(),
                conversation.getLastMessageAt(),
                null
        );
    }

    /**
     * Mesajlarla birlikte konuşma detayı için DTO üretir.
     *
     * @param conversation entity
     * @param messages     mesaj listesi
     * @return ConversationResponse (messages dolu)
     */
    public ConversationResponse toDetailResponse(Conversation conversation, List<Message> messages) {
        List<ConversationResponse.MessageResponse> msgResponses = messages.stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getSessionId(),
                conversation.getCreatedAt(),
                conversation.getLastMessageAt(),
                msgResponses
        );
    }

    private ConversationResponse.MessageResponse toMessageResponse(Message msg) {
        List<SourceChunkResponse> sources = parseSourceChunks(msg.getSourceChunksJson());
        return new ConversationResponse.MessageResponse(
                msg.getId(), msg.getRole(), msg.getContent(), sources, msg.getCreatedAt());
    }

    private List<SourceChunkResponse> parseSourceChunks(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Failed to parse source_chunks JSON: {}", e.getMessage());
            return List.of();
        }
    }
}

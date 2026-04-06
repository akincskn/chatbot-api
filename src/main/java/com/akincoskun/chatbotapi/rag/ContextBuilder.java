package com.akincoskun.chatbotapi.rag;

import com.akincoskun.chatbotapi.ai.LlmMessage;
import com.akincoskun.chatbotapi.ai.PromptBuilder;
import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RAG pipeline için tam LLM mesaj listesi oluşturur.
 * Sistem prompt + retrieved context + sliding window history + kullanıcı sorusu.
 */
@Component
@RequiredArgsConstructor
public class ContextBuilder {

    /** Sliding window: son kaç mesaj geçmiş olarak alınsın */
    private static final int HISTORY_WINDOW = 5;

    private final PromptBuilder promptBuilder;

    /**
     * Tam mesaj listesi oluşturur.
     *
     * @param chatbot        chatbot entity
     * @param similarChunks  semantic search sonuçları
     * @param recentMessages konuşma geçmişi (en yeni önce — DB'den DESC gelir)
     * @param userQuestion   kullanıcının sorusu
     * @return LLM'e gönderilecek mesaj listesi
     */
    public List<LlmMessage> build(
            Chatbot chatbot,
            List<SimilarChunkResult> similarChunks,
            List<Message> recentMessages,
            String userQuestion) {

        List<LlmMessage> messages = new ArrayList<>();

        // 1. System prompt + context
        String systemPrompt = promptBuilder.buildSystemPrompt(chatbot, similarChunks);
        messages.add(new LlmMessage("system", systemPrompt));

        // 2. Conversation history (son HISTORY_WINDOW mesaj, kronolojik sıra)
        List<Message> historySlice = recentMessages.stream()
                .limit(HISTORY_WINDOW)
                .toList();
        List<Message> chronological = new ArrayList<>(historySlice);
        Collections.reverse(chronological);

        for (Message msg : chronological) {
            String llmRole = "user".equals(msg.getRole()) ? "user" : "assistant";
            messages.add(new LlmMessage(llmRole, msg.getContent()));
        }

        // 3. Kullanıcının sorusu
        messages.add(new LlmMessage("user", userQuestion));

        return messages;
    }
}

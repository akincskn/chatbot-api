package com.akincoskun.chatbotapi.ai;

import com.akincoskun.chatbotapi.entity.Chatbot;
import com.akincoskun.chatbotapi.rag.SimilarChunkResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM için sistem prompt'u oluşturur.
 * Chatbot'un system_prompt'unu, persona tonunu ve retrieved chunk'ları birleştirir.
 */
@Component
public class PromptBuilder {

    private static final String ANTI_HALLUCINATION =
            "\n\nIMPORTANT: Answer ONLY based on the provided context below. " +
            "If the answer is not found in the context, say: " +
            "\"I don't have information about that topic in my knowledge base.\" " +
            "Do NOT make up information or use knowledge outside the provided context.";

    private static final String CITE_SOURCES =
            "\nWhen possible, reference the document or section your answer comes from.";

    /**
     * Sistem prompt'unu oluşturur.
     *
     * @param chatbot        chatbot entity (system_prompt, persona)
     * @param similarChunks  semantic search ile bulunan chunk'lar
     * @return LLM'e gönderilecek sistem mesajı
     */
    public String buildSystemPrompt(Chatbot chatbot, List<SimilarChunkResult> similarChunks) {
        StringBuilder sb = new StringBuilder();

        sb.append(chatbot.getSystemPrompt());
        sb.append(ANTI_HALLUCINATION);
        sb.append(CITE_SOURCES);
        sb.append(toneInstruction(chatbot.getPersona()));

        if (!similarChunks.isEmpty()) {
            sb.append("\n\n--- CONTEXT ---\n");
            for (int i = 0; i < similarChunks.size(); i++) {
                SimilarChunkResult chunk = similarChunks.get(i);
                sb.append(String.format("[Source %d: %s]\n%s\n\n",
                        i + 1, chunk.filename(), chunk.content()));
            }
            sb.append("--- END CONTEXT ---");
        } else {
            sb.append("\n\n[No relevant context found in the knowledge base.]");
        }

        return sb.toString();
    }

    private String toneInstruction(String persona) {
        return switch (persona == null ? "professional" : persona) {
            case "friendly" -> "\n\nTone: Be warm, friendly and approachable. Use simple language.";
            case "casual"   -> "\n\nTone: Be casual and conversational. Use informal language.";
            default         -> "\n\nTone: Be professional, clear and concise.";
        };
    }
}

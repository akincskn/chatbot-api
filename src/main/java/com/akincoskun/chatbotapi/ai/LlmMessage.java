package com.akincoskun.chatbotapi.ai;

/**
 * LLM'e gönderilen tek bir mesaj.
 *
 * @param role    "system" | "user" | "assistant"
 * @param content mesaj içeriği
 */
public record LlmMessage(String role, String content) {}

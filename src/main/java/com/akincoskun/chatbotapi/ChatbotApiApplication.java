package com.akincoskun.chatbotapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Chatbot Platform — Spring Boot entry point.
 * RAG-powered chatbot backend with JWT auth and pgvector support.
 */
@SpringBootApplication
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class ChatbotApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApiApplication.class, args);
    }
}

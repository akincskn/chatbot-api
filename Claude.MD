# CLAUDE.md — Chatbot API (Spring Boot Backend)

## Proje Nedir?
AI Chatbot Platform'un backend API'ı. Kullanıcılar döküman yükler (PDF, URL, text), sistem bunları vectorize eder ve RAG (Retrieval Augmented Generation) ile soru-cevap chatbot'u oluşturur. Chatbot'lar embed script ile başka sitelere eklenebilir.

## Tech Stack
- Java 21 + Spring Boot 3.x
- Spring Security + JWT (stateless auth)
- Spring Data JPA + Hibernate
- PostgreSQL + pgvector extension (Neon)
- Groq API (Llama 3.3 — primary LLM)
- Google Gemini API (fallback LLM)
- HuggingFace Inference API (embedding — all-MiniLM-L6-v2)
- Apache PDFBox (PDF parsing)
- Jsoup (HTML parsing / web scraping)
- Maven
- Deploy: Koyeb (free tier)

## Mimari Kuralları

### Katmanlı Mimari (ZORUNLU)
```
Controller → Service → Repository
```
- Controller: HTTP layer, input validation (@Valid), auth kontrolü, DTO dönüşümü
- Service: Business logic, transaction yönetimi, external service çağrıları
- Repository: JPA data access, native query (pgvector)
- **Controller ASLA doğrudan Repository çağırmaz**
- **Repository ASLA business logic içermez**

### DTO Pattern (ZORUNLU)
- Entity'ler doğrudan response olarak DÖNMEZ
- Request DTO: kullanıcıdan gelen veri (@Valid annotated)
- Response DTO: kullanıcıya dönen veri (sadece gereken alanlar)
- Mapper class: Entity ↔ DTO dönüşümü

### Paket Yapısı
```
com.akincoskun.chatbotapi/
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── JwtConfig.java
│   └── WebConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ChatbotController.java
│   ├── DocumentController.java
│   └── ChatController.java
├── service/
│   ├── AuthService.java
│   ├── ChatbotService.java
│   ├── DocumentService.java
│   ├── ChatService.java
│   ├── EmbeddingService.java
│   └── TextSplitterService.java
├── repository/
│   ├── UserRepository.java
│   ├── ChatbotRepository.java
│   ├── DocumentRepository.java
│   ├── DocumentChunkRepository.java
│   ├── ConversationRepository.java
│   └── MessageRepository.java
├── entity/
│   ├── User.java
│   ├── Chatbot.java
│   ├── Document.java
│   ├── DocumentChunk.java
│   ├── Conversation.java
│   └── Message.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── CreateChatbotRequest.java
│   │   ├── UploadDocumentRequest.java
│   │   └── ChatMessageRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── ChatbotResponse.java
│       ├── DocumentResponse.java
│       ├── ChatMessageResponse.java
│       └── ConversationResponse.java
├── mapper/
│   ├── ChatbotMapper.java
│   ├── DocumentMapper.java
│   └── ConversationMapper.java
├── ai/
│   ├── GroqClient.java
│   ├── GeminiClient.java
│   ├── HuggingFaceEmbeddingClient.java
│   ├── LlmService.java (fallback logic)
│   └── PromptBuilder.java
├── rag/
│   ├── TextSplitter.java
│   ├── RagPipeline.java
│   └── ContextBuilder.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── CreditExhaustedException.java
└── util/
    ├── UrlValidator.java
    └── TextCleaner.java
```

### Kod Kuralları
- **Java 21** — record types, pattern matching, text blocks kullan
- **Dosya başına max 200 satır** — aşarsa böl
- **Her public method'da Javadoc** — en azından @param ve @return
- **Magic number/string YASAK** — constants class'a çıkar
- **System.out.println YASAK** — SLF4J Logger kullan
- **Null return YASAK** — Optional kullan veya exception fırlat
- **Raw type YASAK** — Generic type'ları explicit belirt
- **Field injection YASAK** — Constructor injection kullan (@RequiredArgsConstructor)

### Güvenlik Kuralları
- Tüm endpoint'ler JWT ile korunacak (/api/auth/** hariç)
- /api/chat/public/** endpoint'i JWT gerektirmez (embed script için)
- Password: BCrypt hash, min 8 karakter
- CORS: Frontend URL'e izin ver
- Rate limiting: chat endpoint'ine IP bazlı (RestTemplate + bucket4j veya in-memory)
- SSRF koruması: URL fetch'te localhost/private IP engelle
- Input validation: @Valid + Jakarta Validation annotations

### Hata Yönetimi
- GlobalExceptionHandler ile merkezi hata yönetimi (@RestControllerAdvice)
- Her hata standardize ResponseEntity döner: { error, message, status, timestamp }
- Business exception'lar custom exception class'larla
- AI hatalarında fallback: Groq → Gemini
- Beklenmeyen hatalar 500 döner, internal detay SIZMAZ

### Test
- Her Service class'ı için unit test (@SpringBootTest veya Mockito)
- Controller'lar MockMvc ile test
- Test yazılmasa bile test edilebilir kod yaz (dependency injection, interface'ler)

## Referans Dosyalar
- docs/ARCHITECTURE.md → Mimari detayları
- docs/DATABASE.md → DB şeması
- docs/CHECKLIST.md → Implementation planı
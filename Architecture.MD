# ARCHITECTURE.md — AI Chatbot Platform

## Sistem Genel Bakışı

AI Chatbot Platform, kullanıcıların kendi verilerine dayalı chatbot'lar oluşturmasını sağlar. Kullanıcı döküman yükler (PDF, URL, text), sistem RAG (Retrieval Augmented Generation) ile bu verilere dayalı soru-cevap yapan bir chatbot oluşturur. Chatbot embed script ile herhangi bir web sitesine eklenebilir.

## Sistem Mimarisi

```
┌──────────────────────────────────────────────────────────┐
│            FRONTEND (Vercel — Next.js 14)                 │
│                                                          │
│  / Landing Page                                          │
│  /login, /register — Auth sayfaları                      │
│  /dashboard — Chatbot listesi + istatistikler            │
│  /chatbot/new — Yeni chatbot oluşturma                   │
│  /chatbot/[id] — Chatbot yönetimi (döküman, ayarlar)     │
│  /chatbot/[id]/chat — Test chat arayüzü                  │
│  /chatbot/[id]/embed — Embed kodu alma                   │
│                                                          │
│  Auth: NextAuth.js v5 → JWT token alır → API'ya gönderir │
└──────────────────┬───────────────────────────────────────┘
                   │ REST API (JWT Bearer token)
                   ▼
┌──────────────────────────────────────────────────────────┐
│          BACKEND (Koyeb — Spring Boot 3.x)               │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Auth Endpoints (/api/auth/**)                      │  │
│  │ POST /api/auth/register — Kayıt                    │  │
│  │ POST /api/auth/login — JWT token al                │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Chatbot Endpoints (/api/chatbots/**) [JWT]         │  │
│  │ GET    /api/chatbots — Kullanıcının chatbot'ları   │  │
│  │ POST   /api/chatbots — Yeni chatbot oluştur        │  │
│  │ GET    /api/chatbots/:id — Chatbot detay           │  │
│  │ PUT    /api/chatbots/:id — Chatbot güncelle        │  │
│  │ DELETE /api/chatbots/:id — Chatbot sil             │  │
│  │ GET    /api/chatbots/:id/embed — Embed kodu al     │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Document Endpoints (/api/chatbots/:id/documents)   │  │
│  │ POST   /upload — Döküman yükle (PDF/URL/text)      │  │
│  │ GET    / — Döküman listesi                         │  │
│  │ DELETE /:docId — Döküman sil                       │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Chat Endpoints                                     │  │
│  │ POST /api/chat/:chatbotId [JWT] — Owner test chat  │  │
│  │ POST /api/chat/public/:chatbotId — Embed chat      │  │
│  │      (JWT gerektirmez — embed script için)          │  │
│  │ GET  /api/chat/:chatbotId/conversations [JWT]      │  │
│  │ GET  /api/chat/conversation/:convId [JWT]          │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Embed Script Endpoint                              │  │
│  │ GET /embed.js — JavaScript widget dosyası          │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│        DATABASE (Neon PostgreSQL + pgvector)              │
│                                                          │
│  Extension: CREATE EXTENSION IF NOT EXISTS vector;       │
│                                                          │
│  Tables: users, chatbots, documents, document_chunks,    │
│          conversations, messages                         │
│                                                          │
│  Vector search:                                          │
│  SELECT content, 1 - (embedding <=> ?) AS similarity     │
│  FROM document_chunks                                    │
│  WHERE document_id IN (chatbot's documents)              │
│  ORDER BY embedding <=> ?                                │
│  LIMIT 5                                                 │
└──────────────────────────────────────────────────────────┘
```

## RAG Pipeline Detayı

### Döküman İşleme Akışı
```
Kullanıcı döküman yükler
       ↓
DocumentController.upload()
  → @Valid ile input validation
  → Auth kontrolü (JWT → userId)
  → Chatbot ownership kontrolü
       ↓
DocumentService.processDocument()
  → source_type'a göre parse:
    - PDF → PDFBox ile text çıkar
    - URL → Jsoup ile fetch + text çıkar (10s timeout, SSRF koruması)
    - TEXT → Direkt kullan
       ↓
TextSplitter.split()
  → Recursive character splitting
  → Chunk size: 500 token
  → Chunk overlap: 50 token
  → Her chunk'a metadata ekle (document_id, chunk_index)
       ↓
EmbeddingService.embed()
  → HuggingFace Inference API çağrısı
  → Model: sentence-transformers/all-MiniLM-L6-v2
  → Input: chunk text → Output: float[384] vector
  → Batch processing (her seferde 5 chunk)
       ↓
DocumentChunkRepository.saveAll()
  → content (TEXT) + embedding (VECTOR(384)) + metadata
       ↓
Document status → "ready"
```

### Soru-Cevap Akışı (Chat)
```
Kullanıcı soru sorar
       ↓
ChatController.chat()
  → Input validation
  → Auth veya public access kontrolü
       ↓
ChatService.processMessage()
       ↓
Step 1: Query Embedding
  → EmbeddingService.embed(userQuestion)
  → float[384] vector
       ↓
Step 2: Semantic Search (pgvector)
  → DocumentChunkRepository.findSimilarChunks(
      chatbotId, queryVector, limit=5, minSimilarity=0.3)
  → En benzer 5 chunk döner
       ↓
Step 3: Context Building
  → ContextBuilder.build(
      systemPrompt,      // ~200 token
      retrievedChunks,   // ~2000 token (5 × 400)
      conversationHistory, // ~1500 token (son 5 mesaj)
      userQuestion        // ~100 token
    )
  → Total: ~4000 token (Groq limit'e uygun)
       ↓
Step 4: LLM Call
  → LlmService.generate(context)
  → Primary: Groq (Llama 3.3 70B)
  → Fallback: Gemini (2.0 Flash)
  → Streaming response (SSE)
       ↓
Step 5: Save
  → Message kaydedilir (role, content, source_chunks)
  → Conversation güncellenir (last_message_at)
       ↓
Response → Frontend'e streaming
```

## Context Window Yönetimi

```
┌─────────────────────────────────────────┐
│           Context Window (~4000 token)   │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ System Prompt (~200 token)      │    │
│  │ "You are a helpful assistant.   │    │
│  │  Answer based ONLY on the       │    │
│  │  provided context..."           │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ Retrieved Chunks (~2000 token)  │    │
│  │ [chunk 1] similarity: 0.89     │    │
│  │ [chunk 2] similarity: 0.82     │    │
│  │ [chunk 3] similarity: 0.75     │    │
│  │ [chunk 4] similarity: 0.71     │    │
│  │ [chunk 5] similarity: 0.65     │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ Conversation History (~1500 tk) │    │
│  │ User: "What is X?"             │    │
│  │ Assistant: "X is..."           │    │
│  │ User: "Tell me more"           │    │
│  │ Assistant: "Additionally..."    │    │
│  │ → Sliding window: son 5 mesaj  │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ User Question (~100 token)      │    │
│  │ "What is the refund policy?"    │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

## Embedding ve Vector Search

### Neden all-MiniLM-L6-v2?
- Ücretsiz (HuggingFace Inference API)
- 384 boyutlu vector (küçük, hızlı)
- İngilizce + çoklu dil desteği
- Semantic similarity'de güçlü performans

### pgvector Kullanımı
```sql
-- Extension etkinleştirme
CREATE EXTENSION IF NOT EXISTS vector;

-- Tablo oluşturma
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id),
    content TEXT NOT NULL,
    embedding VECTOR(384),
    chunk_index INTEGER,
    token_count INTEGER
);

-- Index (performans için)
CREATE INDEX ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- Similarity search
SELECT content, 1 - (embedding <=> :queryVector) AS similarity
FROM document_chunks
WHERE document_id IN (
    SELECT id FROM documents WHERE chatbot_id = :chatbotId
)
AND 1 - (embedding <=> :queryVector) > 0.3
ORDER BY embedding <=> :queryVector
LIMIT 5;
```

## Auth Akışı

```
Frontend (NextAuth.js)           Backend (Spring Security)
       │                                │
  1. Google OAuth login                 │
       │                                │
  2. NextAuth session oluşturur         │
       │                                │
  3. POST /api/auth/login               │
     { email, name, googleId }  ───────►│
       │                                │
       │                     4. User yoksa oluştur
       │                        JWT token üret
       │                                │
       │◄─────────── { token, user } ───┤
       │                                │
  5. Token'ı localStorage'da sakla      │
       │                                │
  6. Her API isteğinde:                 │
     Authorization: Bearer <token> ────►│
       │                                │
       │                     7. JwtAuthFilter token'ı doğrular
       │                        SecurityContext'e user koyar
```

## Embed Script Mimarisi

```
Embed kodu (müşterinin sitesinde):
<script src="https://chatbot-api.koyeb.app/embed.js" 
        data-chatbot-id="abc123"></script>

embed.js ne yapar:
1. DOM'a chat widget ekler (iframe veya shadow DOM)
2. Sağ alta chat baloncuğu koyar
3. Tıklanınca chat penceresi açılır
4. POST /api/chat/public/:chatbotId endpoint'ine mesaj gönderir
5. Streaming response alır (SSE)
6. Session ID ile conversation takip eder (localStorage)
```

## Maliyet Tablosu

| Servis | Free Tier Limiti | Yeterli mi? |
|--------|-----------------|-------------|
| Koyeb | 512MB RAM, 0.1 vCPU | Spring Boot için sıkı ama MVP yeter |
| Neon | 0.5GB, 190 saat compute | Yeter |
| Groq | 30 req/dk, 14.4K token/dk | Yeter |
| Gemini | 15 req/dk, 1500 req/gün | Yeter (fallback) |
| HuggingFace | Rate limited ama ücretsiz | Yeter |
| Vercel | 100GB bandwidth | Yeter |
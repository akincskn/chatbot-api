package com.akincoskun.chatbotapi.exception;

/**
 * Rate limit aşıldığında fırlatılır. 429 HTTP yanıtına map edilir.
 */
public class RateLimitExceededException extends RuntimeException {

    /**
     * @param message hata mesajı
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}

package com.akincoskun.chatbotapi.exception;

/**
 * Kullanıcının kredisi bittiğinde fırlatılır. 402 HTTP yanıtına map edilir.
 */
public class CreditExhaustedException extends RuntimeException {

    /**
     * @param message hata mesajı
     */
    public CreditExhaustedException(String message) {
        super(message);
    }
}

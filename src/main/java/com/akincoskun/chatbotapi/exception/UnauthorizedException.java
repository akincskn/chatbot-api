package com.akincoskun.chatbotapi.exception;

/**
 * Kullanıcı yetkisiz bir kaynağa erişmeye çalıştığında fırlatılır. 403 HTTP yanıtına map edilir.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * @param message hata mesajı
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}

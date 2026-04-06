package com.akincoskun.chatbotapi.exception;

/**
 * İstenen kaynak bulunamadığında fırlatılır. 404 HTTP yanıtına map edilir.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message hata mesajı (kullanıcıya gösterilir)
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

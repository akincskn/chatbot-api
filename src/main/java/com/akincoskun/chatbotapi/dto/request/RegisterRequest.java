package com.akincoskun.chatbotapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Kayıt isteği DTO'su.
 *
 * @param email    geçerli e-posta adresi (unique)
 * @param password minimum 8 karakter
 * @param name     kullanıcının görünen adı
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name is too long")
        String name
) {}

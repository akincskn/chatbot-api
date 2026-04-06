package com.akincoskun.chatbotapi.controller;

import com.akincoskun.chatbotapi.dto.request.LoginRequest;
import com.akincoskun.chatbotapi.dto.request.RegisterRequest;
import com.akincoskun.chatbotapi.dto.response.AuthResponse;
import com.akincoskun.chatbotapi.service.AuthService;
import com.akincoskun.chatbotapi.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoint'leri. JWT gerektirmez (/api/auth/** public).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    /**
     * Yeni kullanıcı kaydı. Başarılı olursa JWT token döner.
     *
     * @param request email, password, name
     * @return 201 + AuthResponse
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Kullanıcı girişi. Geçerli kimlik bilgileriyle JWT token döner.
     * IP başına dakikada 5 deneme ile rate limit uygulanır.
     *
     * @param request    email, password
     * @param httpRequest istemci IP tespiti için
     * @return 200 + AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        rateLimiterService.checkLogin(httpRequest.getRemoteAddr());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

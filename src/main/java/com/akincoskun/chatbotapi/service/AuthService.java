package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.dto.request.LoginRequest;
import com.akincoskun.chatbotapi.dto.request.RegisterRequest;
import com.akincoskun.chatbotapi.dto.response.AuthResponse;
import com.akincoskun.chatbotapi.entity.User;
import com.akincoskun.chatbotapi.repository.UserRepository;
import com.akincoskun.chatbotapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcı kayıt ve giriş işlemlerini yönetir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Yeni kullanıcı kaydeder ve JWT token döner.
     *
     * @param request kayıt bilgileri
     * @return token ve kullanıcı bilgisi
     * @throws IllegalArgumentException e-posta zaten kayıtlıysa
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .credits(3)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        String token = jwtTokenProvider.generateToken(saved.getEmail());
        return toAuthResponse(saved, token);
    }

    /**
     * E-posta ve şifre ile giriş yapar, JWT token döner.
     *
     * @param request giriş bilgileri
     * @return token ve kullanıcı bilgisi
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after auth"));

        String token = jwtTokenProvider.generateToken(user.getEmail());
        log.info("User logged in: {}", user.getEmail());
        return toAuthResponse(user, token);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getCredits());
    }
}

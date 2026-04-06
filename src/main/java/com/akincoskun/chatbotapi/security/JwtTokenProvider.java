package com.akincoskun.chatbotapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token üretme, doğrulama ve parse işlemlerini yönetir.
 * jjwt 0.12.x API kullanır.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Uygulama başlarken JWT secret'ın yeterince uzun olduğunu doğrular.
     * HS256 için minimum 32 byte (256 bit) gereklidir.
     */
    @PostConstruct
    public void validateSecret() {
        int len = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (len < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256. " +
                    "Current length: " + len + " bytes. Set a strong JWT_SECRET environment variable.");
        }
    }

    /**
     * Kullanıcı e-postasından JWT token üretir.
     *
     * @param email token subject olarak kullanılacak e-posta
     * @return imzalanmış JWT string
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Token geçerliyse subject (email) döner, aksi hâlde null.
     *
     * @param token JWT string
     * @return e-posta adresi veya null
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Token'ın geçerli ve süresi dolmamış olup olmadığını kontrol eder.
     *
     * @param token JWT string
     * @return geçerliyse true
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

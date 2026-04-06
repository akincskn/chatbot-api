package com.akincoskun.chatbotapi.service;

import com.akincoskun.chatbotapi.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP ve kullanıcı bazlı sliding window rate limiter.
 * Durum bellekte tutulur; uygulama yeniden başladığında sıfırlanır.
 *
 * <p>Limitler:
 * <ul>
 *   <li>Public (IP bazlı): 20 istek/dakika</li>
 *   <li>Authenticated (kullanıcı bazlı): 60 istek/dakika</li>
 * </ul>
 */
@Service
@Slf4j
public class RateLimiterService {

    private static final int PUBLIC_LIMIT = 20;
    private static final int AUTH_LIMIT = 60;
    private static final int LOGIN_LIMIT = 5;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> store = new ConcurrentHashMap<>();

    /**
     * Public endpoint için IP bazlı limit kontrolü.
     *
     * @param ip istemci IP adresi
     * @throws RateLimitExceededException limit aşıldığında
     */
    public void checkPublic(String ip) {
        checkLimit("pub:" + ip, PUBLIC_LIMIT);
    }

    /**
     * JWT endpoint için kullanıcı bazlı limit kontrolü.
     *
     * @param userEmail kimliği doğrulanmış kullanıcının e-postası
     * @throws RateLimitExceededException limit aşıldığında
     */
    public void checkAuthenticated(String userEmail) {
        checkLimit("auth:" + userEmail, AUTH_LIMIT);
    }

    /**
     * Login endpoint için IP bazlı brute force koruması (5 deneme/dakika).
     *
     * @param ip istemci IP adresi
     * @throws RateLimitExceededException limit aşıldığında
     */
    public void checkLogin(String ip) {
        checkLimit("login:" + ip, LOGIN_LIMIT);
    }

    private void checkLimit(String key, int maxRequests) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = store.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                throw new RateLimitExceededException(
                        "Rate limit exceeded: max " + maxRequests + " requests per minute. Try again later.");
            }
            timestamps.addLast(now);
        }
    }

    /** Eski kayıtları her 5 dakikada temizler (bellek sızıntısı önlemi). */
    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        store.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                while (!entry.getValue().isEmpty() && entry.getValue().peekFirst() < cutoff) {
                    entry.getValue().pollFirst();
                }
                return entry.getValue().isEmpty();
            }
        });
        log.debug("Rate limiter cleanup done. Active keys: {}", store.size());
    }
}

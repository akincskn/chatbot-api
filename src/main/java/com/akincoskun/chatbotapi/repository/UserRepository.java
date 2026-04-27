package com.akincoskun.chatbotapi.repository;

import com.akincoskun.chatbotapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * User veri erişim katmanı.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * @param email kullanıcının e-posta adresi
     * @return kullanıcı varsa Optional içinde döner
     */
    Optional<User> findByEmail(String email);

    /**
     * @param email kontrol edilecek e-posta
     * @return kayıtlı mı
     */
    boolean existsByEmail(String email);

    /**
     * @param googleId Google OAuth ID
     * @return kullanıcı varsa Optional içinde döner
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Kullanıcının kredisini atomik olarak 1 düşürür (yalnızca kredi > 0 ise).
     *
     * @param userId kullanıcı UUID
     * @return güncellenen satır sayısı (0 ise kredi yetersiz)
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.credits = u.credits - 1 WHERE u.id = :userId AND u.credits > 0")
    int decrementCredits(@Param("userId") UUID userId);
}

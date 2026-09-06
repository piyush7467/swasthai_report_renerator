package com.swasthai.report_generator.auth.repository;

import com.swasthai.report_generator.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    /*
     * Find a reset token using its SHA-256 hash.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /*
     * Find only a valid, unused and non-expired token.
     */
    Optional<PasswordResetToken>
    findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            Instant currentTime
    );

    /*
     * Check whether a token hash already exists.
     */
    boolean existsByTokenHash(String tokenHash);

    /*
     * Remove expired reset tokens.
     *
     * Reset tokens are short-lived and do not need
     * indefinite database retention.
     */
    long deleteByExpiresAtBefore(Instant currentTime);

    /*
     * Remove all existing reset tokens for a user.
     *
     * Useful when generating a new reset request so
     * previously issued reset links become invalid.
     */
    long deleteByUser_Id(UUID userId);
}
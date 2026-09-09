package com.swasthai.report_generator.auth.repository;

import com.swasthai.report_generator.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    /*
     * Find a refresh token using its hashed value.
     *
     * The raw refresh token is never stored in the database.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(
            @org.springframework.data.repository.query.Param("tokenHash") String tokenHash
    );

    /*
     * Find a token only if it belongs to the specified user.
     *
     * Useful for tenant/user-safe operations.
     */
    Optional<RefreshToken> findByTokenHashAndUser_Id(
            String tokenHash,
            UUID userId
    );

    /*
     * Check whether a token hash already exists.
     *
     * Mainly useful for defensive uniqueness checks.
     */
    boolean existsByTokenHash(String tokenHash);

    /*
     * Find active, non-expired refresh tokens for a user.
     *
     * A token is active when:
     * - it has not been revoked
     * - it has not been rotated
     * - it has not expired
     */
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndRotatedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            Instant currentTime
    );

    /*
     * Delete expired/revoked tokens during cleanup.
     */
    long deleteByExpiresAtBefore(Instant currentTime);

    /*
     * Revoke all refresh tokens belonging to a user.
     *
     * We'll use this when:
     * - password is changed
     * - password is reset
     * - token reuse is detected
     * - account security requires forced logout
     */
    long deleteByUser_Id(UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    int revokeAllActiveByUserId(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("now") Instant now
    );
}
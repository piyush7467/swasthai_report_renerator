package com.swasthai.report_generator.auth.entity;

import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refresh_token_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_refresh_token_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_refresh_token_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_refresh_token_expires_at",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_refresh_token_revoked_at",
                        columnList = "revoked_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /*
     * Internal database ID.
     *
     * Never expose this through the API.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Public reference ID.
     *
     * Useful for internal audit/reference purposes.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String refId;

    /*
     * User who owns this refresh token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_refresh_token_user"
            )
    )
    private User user;

    /*
     * SHA-256 hash of the actual refresh token.
     *
     * NEVER store the raw refresh token in the database.
     */
    @Column(
            name = "token_hash",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String tokenHash;

    /*
     * Token expiration time.
     */
    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    /*
     * When the token was revoked.
     *
     * NULL means it has not been revoked.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /*
     * Token creation time.
     */
    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /*
     * Optional timestamp for token rotation.
     *
     * When an old refresh token is exchanged for a new one,
     * this field can be used to record the rotation event.
     */
    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;

        if (refId == null || refId.isBlank()) {
            refId = generateRefId();
        }
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator.generate("RT");
    }
}
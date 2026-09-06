package com.swasthai.report_generator.auth.entity;

import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_password_reset_token_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_password_reset_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_password_reset_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_reset_expires_at",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_password_reset_used_at",
                        columnList = "used_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /*
     * Internal database ID.
     *
     * Never expose this through the API.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Public reference ID for internal/audit purposes.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String refId;

    /*
     * User requesting the password reset.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_password_reset_user"
            )
    )
    private User user;

    /*
     * SHA-256 hash of the actual reset token.
     *
     * Never store the raw reset token.
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
     * Time at which the token was consumed.
     *
     * NULL means the token has not been used.
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /*
     * Creation timestamp.
     */
    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (refId == null || refId.isBlank()) {
            refId = generateRefId();
        }
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator.generate("PRT");
    }
}
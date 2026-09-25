package com.swasthai.report_generator.user.entity;

import com.swasthai.report_generator.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_user_organization",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_user_org_role",
                        columnList = "organization_id, role"
                ),
                @Index(
                        name = "idx_user_org_status",
                        columnList = "organization_id, status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /*
     * Internal database ID.
     *
     * Never expose this through API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Public identifier.
     *
     * Used by APIs instead of exposing the internal database ID.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String refId;

    /*
     * User's display name.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    /*
     * Login email.
     *
     * Normalized to lowercase before persistence.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String email;

    /*
     * BCrypt/Argon2 password hash.
     *
     * Never store plain-text passwords.
     */
    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    /*
     * Application role.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private Role role;

    /*
     * Account status.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /*
     * Organization/clinic.
     *
     * SUPER_ADMIN -> null
     * ORG_ADMIN   -> required
     * LAB_STAFF   -> required
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "organization_id",
            foreignKey = @ForeignKey(
                    name = "fk_user_organization"
            )
    )
    private Organization organization;

    /*
     * Last successful login.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /*
     * Deactivation timestamp (eligible for permanent cleanup 10 days after this).
     */
    @Column(name = "inactive_at")
    private Instant inactiveAt;

    /*
     * Administrator who deactivated this account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deactivated_by",
            foreignKey = @ForeignKey(
                    name = "fk_users_deactivated_by"
            )
    )
    private User deactivatedBy;

    /*
     * Optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /*
     * Creation timestamp.
     */
    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /*
     * Last modification timestamp.
     */
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = generateRefId();
        }

        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();

        normalizeFields();
    }

    private void normalizeFields() {

        if (name != null) {
            name = name.trim();
        }

        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator.generate("USR");
    }
}
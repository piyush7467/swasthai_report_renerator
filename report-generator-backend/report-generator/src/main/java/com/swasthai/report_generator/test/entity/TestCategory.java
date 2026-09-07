package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_categories", uniqueConstraints = {
                @UniqueConstraint(name = "uk_test_category_ref_id", columnNames = "ref_id"),
                @UniqueConstraint(name = "uk_test_category_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_test_category_name", columnNames = "name")
}, indexes = {
                @Index(name = "idx_test_category_status", columnList = "status"),
                @Index(name = "idx_test_category_ref_id", columnList = "ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCategory {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 30)
        private String refId;

        @Column(nullable = false, length = 50)
        private String code;

        @Column(nullable = false, length = 100)
        private String name;

        @Column(length = 500)
        private String description;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        @Builder.Default
        private TestCategoryStatus status = TestCategoryStatus.ACTIVE;

        @Column(nullable = false, updatable = false)
        private Instant createdAt;

        @Column(nullable = false)
        private Instant updatedAt;

        // --------------------------------------------------
        // Lifecycle
        // --------------------------------------------------

        @PrePersist
        protected void onCreate() {

                Instant now = Instant.now();

                createdAt = now;
                updatedAt = now;

                if (refId == null || refId.isBlank()) {
                        refId = RefIdGenerator.generate("TC");
                }

                normalizeFields();
        }

        @PreUpdate
        protected void onUpdate() {

                updatedAt = Instant.now();

                normalizeFields();
        }

        // --------------------------------------------------
        // Field Normalization
        // --------------------------------------------------

        private void normalizeFields() {

                if (code != null) {
                        code = code.trim().toUpperCase();
                }

                if (name != null) {
                        name = name.trim();
                }

                if (description != null) {

                        description = description.trim();

                        if (description.isBlank()) {
                                description = null;
                        }
                }
        }
}
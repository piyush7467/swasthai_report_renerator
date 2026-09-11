package com.swasthai.report_generator.common.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limit_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRateLimitAttempt {

    @Id
    @Column(name = "key_hash", length = 64, nullable = false)
    private String keyHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "window_start_epoch_second", nullable = false)
    private long windowStartEpochSecond;

    @Column(name = "locked_until_epoch_second", nullable = false)
    private long lockedUntilEpochSecond;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

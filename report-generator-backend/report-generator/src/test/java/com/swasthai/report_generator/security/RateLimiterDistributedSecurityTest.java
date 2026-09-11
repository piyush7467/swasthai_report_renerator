package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.service.AuthService;
import com.swasthai.report_generator.common.security.DistributedRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.swasthai.report_generator.common.security.entity.AuthRateLimitAttempt;
import com.swasthai.report_generator.common.security.repository.AuthRateLimitAttemptRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RateLimiterDistributedSecurityTest {

    @Autowired
    private DistributedRateLimiter distributedRateLimiter;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthRateLimitAttemptRepository rateLimitAttemptRepository;

    private String testKey;

    @BeforeEach
    void setUp() {
        testKey = "test:auth:" + UUID.randomUUID();
    }

    @Test
    @DisplayName("SEC-AUTH-001: Rate limiter enforces maximum attempt threshold and locks out")
    void testThresholdEnforced() {
        int maxAttempts = 3;
        long windowSec = 60;
        long lockSec = 120;

        // Attempts 1, 2, 3 allowed
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();

        // 4th attempt exceeds maxAttempts -> locked out
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isFalse();

        // 5th attempt during lockout is still rejected
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isFalse();
    }

    @Test
    @DisplayName("SEC-AUTH-001: Window expiration resets the attempt count")
    void testWindowExpiration() {
        int maxAttempts = 2;
        long windowSec = 1; // 1 second window
        long lockSec = 5;

        // Make 2 attempts (max allowed within window)
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();

        // Wait 1.5 seconds for window to roll over
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {}

        // After window expiration, new attempt succeeds and resets window
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
    }

    @Test
    @DisplayName("SEC-AUTH-001: Reset explicitly clears locked rate limiter key")
    void testResetClearsLimit() {
        int maxAttempts = 2;
        long windowSec = 60;
        long lockSec = 120;

        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isFalse();

        distributedRateLimiter.reset(testKey);

        // Immediately allowed after reset
        assertThat(distributedRateLimiter.checkAndIncrement(testKey, maxAttempts, windowSec, lockSec)).isTrue();
    }

    @Test
    @DisplayName("SEC-AUTH-001: Attacker changing IPs cannot bypass account-level lockout")
    void testAccountLockoutIndependentOfIp() {
        String targetEmail = "victim-" + UUID.randomUUID() + "@target.com";

        // Attempt 5 failed logins from 5 different IPs
        for (int i = 1; i <= 5; i++) {
            final String fakeIp = "192.168.1." + i;
            LoginRequest req = new LoginRequest();
            req.setEmail(targetEmail);
            req.setPassword("WrongPassword!");
            assertThatThrownBy(() -> authService.login(req, fakeIp))
                    .isInstanceOf(IllegalArgumentException.class); // Bad credentials
        }

        // 6th attempt from a brand new IP must be rejected by rate limiter (account-level protection)
        String newIp = "10.0.0.99";
        LoginRequest req6 = new LoginRequest();
        req6.setEmail(targetEmail);
        req6.setPassword("WrongPassword!");
        assertThatThrownBy(() -> authService.login(req6, newIp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    @DisplayName("SEC-AUTH-001: Rate limit cleanup removes genuinely stale rows but preserves active/locked rows")
    void testCleanupSafety_RemovesOnlyStaleRecords() {
        long now = Instant.now().getEpochSecond();

        // 1. Genuinely stale record (window expired 2 days ago, not locked)
        String staleKeyHash = "stale_hash_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        rateLimitAttemptRepository.saveAndFlush(AuthRateLimitAttempt.builder()
                .keyHash(staleKeyHash)
                .attemptCount(3)
                .windowStartEpochSecond(now - 172800)
                .lockedUntilEpochSecond(0L)
                .updatedAt(Instant.now().minusSeconds(172800))
                .build());

        // 2. Active window record (window started 10 seconds ago)
        String activeKeyHash = "active_hash_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        rateLimitAttemptRepository.saveAndFlush(AuthRateLimitAttempt.builder()
                .keyHash(activeKeyHash)
                .attemptCount(1)
                .windowStartEpochSecond(now - 10)
                .lockedUntilEpochSecond(0L)
                .updatedAt(Instant.now().minusSeconds(10))
                .build());

        // 3. Currently locked record (locked until 200 seconds in the future)
        String lockedKeyHash = "locked_hash_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        rateLimitAttemptRepository.saveAndFlush(AuthRateLimitAttempt.builder()
                .keyHash(lockedKeyHash)
                .attemptCount(5)
                .windowStartEpochSecond(now - 100)
                .lockedUntilEpochSecond(now + 200)
                .updatedAt(Instant.now().minusSeconds(50))
                .build());

        // Run purge with cutoff = now - 86400 (1 day ago)
        long staleCutoff = now - 86400;
        int purged = distributedRateLimiter.purgeStaleRecords(staleCutoff, 100);

        assertThat(purged).isGreaterThanOrEqualTo(1);

        // Verify state
        assertThat(rateLimitAttemptRepository.findById(staleKeyHash)).isEmpty();
        assertThat(rateLimitAttemptRepository.findById(activeKeyHash)).isPresent();
        assertThat(rateLimitAttemptRepository.findById(lockedKeyHash)).isPresent();

        // Clean up remaining test records
        rateLimitAttemptRepository.deleteById(activeKeyHash);
        rateLimitAttemptRepository.deleteById(lockedKeyHash);
    }
}
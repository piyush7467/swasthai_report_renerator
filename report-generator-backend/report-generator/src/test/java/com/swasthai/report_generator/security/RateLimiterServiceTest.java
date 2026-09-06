package com.swasthai.report_generator.security;

import com.swasthai.report_generator.common.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void testAllowsUpToLimitAndBlocksSubsequent() {
        String key = "test-ip-1";
        int limit = 3;
        long window = 60;

        assertTrue(rateLimiterService.isAllowed(key, limit, window));
        assertTrue(rateLimiterService.isAllowed(key, limit, window));
        assertTrue(rateLimiterService.isAllowed(key, limit, window));

        // 4th request should be blocked
        assertFalse(rateLimiterService.isAllowed(key, limit, window));
    }

    @Test
    void testResetClearsLimit() {
        String key = "test-ip-2";
        int limit = 2;

        assertTrue(rateLimiterService.isAllowed(key, limit, 60));
        assertTrue(rateLimiterService.isAllowed(key, limit, 60));
        assertFalse(rateLimiterService.isAllowed(key, limit, 60));

        rateLimiterService.reset(key);

        // Allowed again after reset
        assertTrue(rateLimiterService.isAllowed(key, limit, 60));
    }

    @Test
    void testPruneExpiredKeysRemovesOldEntries() {
        String key1 = "expired-key";
        String key2 = "active-key";

        rateLimiterService.isAllowed(key1, 5, 1);
        rateLimiterService.isAllowed(key2, 5, 60);

        assertEquals(2, rateLimiterService.getTrackedKeyCount());

        // Pruning with cutoff in future should remove key1
        rateLimiterService.pruneExpiredKeys(java.time.Instant.now().getEpochSecond() + 10);

        assertEquals(0, rateLimiterService.getTrackedKeyCount());
    }
}

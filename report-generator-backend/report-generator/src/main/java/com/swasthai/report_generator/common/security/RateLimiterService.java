package com.swasthai.report_generator.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Enterprise rate limiter supporting distributed atomic counters across application clusters.
 * Falls back to an in-memory sliding window for isolated unit testing environments.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private static final long DEFAULT_WINDOW_SECONDS = 60;
    private static final long DEFAULT_LOCK_SECONDS = 300;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final DistributedRateLimiter distributedRateLimiter;
    private final Map<String, ConcurrentLinkedDeque<Long>> requestLogs;

    @Autowired(required = false)
    public RateLimiterService(DistributedRateLimiter distributedRateLimiter) {
        this.distributedRateLimiter = distributedRateLimiter;
        this.requestLogs = new ConcurrentHashMap<>();
    }

    public RateLimiterService() {
        this.distributedRateLimiter = null;
        this.requestLogs = new ConcurrentHashMap<>();
    }

    public boolean isAllowed(String key) {
        return isAllowed(key, DEFAULT_MAX_ATTEMPTS, DEFAULT_WINDOW_SECONDS, DEFAULT_LOCK_SECONDS);
    }

    public boolean isAllowed(String key, int maxAttempts, long windowSeconds) {
        return isAllowed(key, maxAttempts, windowSeconds, windowSeconds * 5);
    }

    public boolean isAllowed(String key, int maxAttempts, long windowSeconds, long lockSeconds) {
        if (key == null || key.isBlank()) {
            return true;
        }

        if (distributedRateLimiter != null) {
            return distributedRateLimiter.checkAndIncrement(key, maxAttempts, windowSeconds, lockSeconds);
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        // Defensive guard against unbounded memory growth: trigger prune if capacity exceeded
        if (requestLogs.size() >= MAX_TRACKED_KEYS && !requestLogs.containsKey(key)) {
            pruneExpiredKeys(windowStart);
            if (requestLogs.size() >= MAX_TRACKED_KEYS) {
                log.warn("RateLimiter capacity reached ({} keys). Throttling new key: {}", MAX_TRACKED_KEYS, key);
                return false;
            }
        }

        ConcurrentLinkedDeque<Long> timestamps =
                requestLogs.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Evict expired entries for this specific key
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxAttempts) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void reset(String key) {
        if (key != null) {
            requestLogs.remove(key);
            if (distributedRateLimiter != null) {
                distributedRateLimiter.reset(key);
            }
        }
    }

    /**
     * Removes keys whose sliding window has completely expired.
     */
    public synchronized void pruneExpiredKeys(long cutoffEpochSecond) {
        Iterator<Map.Entry<String, ConcurrentLinkedDeque<Long>>> iterator = requestLogs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ConcurrentLinkedDeque<Long>> entry = iterator.next();
            ConcurrentLinkedDeque<Long> deque = entry.getValue();
            while (!deque.isEmpty() && deque.peekFirst() < cutoffEpochSecond) {
                deque.pollFirst();
            }
            if (deque.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public int getTrackedKeyCount() {
        return requestLogs.size();
    }
}


package com.swasthai.report_generator.common.security;

public interface DistributedRateLimiter {

    boolean checkAndIncrement(String key, int maxAttempts, long windowSeconds, long lockSeconds);

    void reset(String key);

    int purgeStaleRecords(long staleCutoffEpochSecond, int batchSize);
}

package com.swasthai.report_generator.common.security;

import com.swasthai.report_generator.common.security.entity.AuthRateLimitAttempt;
import com.swasthai.report_generator.common.security.repository.AuthRateLimitAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseRateLimiterImpl implements DistributedRateLimiter {

    private final AuthRateLimitAttemptRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkAndIncrement(String key, int maxAttempts, long windowSeconds, long lockSeconds) {
        if (key == null || key.isBlank()) {
            return true;
        }

        String keyHash = hashKey(key);
        long now = Instant.now().getEpochSecond();

        Optional<AuthRateLimitAttempt> optionalRecord = repository.findByKeyHashForUpdate(keyHash);

        if (optionalRecord.isPresent()) {
            AuthRateLimitAttempt record = optionalRecord.get();

            // 1. Check if currently locked out
            if (record.getLockedUntilEpochSecond() > now) {
                return false;
            }

            // 2. Check if current window has expired
            if (now - record.getWindowStartEpochSecond() >= windowSeconds) {
                record.setAttemptCount(1);
                record.setWindowStartEpochSecond(now);
                record.setLockedUntilEpochSecond(0L);
                record.setUpdatedAt(Instant.now());
                repository.saveAndFlush(record);
                return true;
            }

            // 3. Within window: increment attempt count
            int newCount = record.getAttemptCount() + 1;
            record.setAttemptCount(newCount);
            record.setUpdatedAt(Instant.now());

            if (newCount > maxAttempts) {
                record.setLockedUntilEpochSecond(now + lockSeconds);
                repository.saveAndFlush(record);
                return false;
            }

            repository.saveAndFlush(record);
            return true;
        } else {
            // Record does not exist: create first entry
            AuthRateLimitAttempt newRecord = AuthRateLimitAttempt.builder()
                    .keyHash(keyHash)
                    .attemptCount(1)
                    .windowStartEpochSecond(now)
                    .lockedUntilEpochSecond(0L)
                    .updatedAt(Instant.now())
                    .build();

            try {
                repository.saveAndFlush(newRecord);
                return 1 <= maxAttempts;
            } catch (DataIntegrityViolationException ex) {
                // Concurrent insert detected: retry lookup with row lock
                return checkAndIncrement(key, maxAttempts, windowSeconds, lockSeconds);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String keyHash = hashKey(key);
        repository.deleteById(keyHash);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeStaleRecords(long staleCutoffEpochSecond, int batchSize) {
        org.springframework.data.domain.Slice<String> slice = repository.findStaleKeyHashes(
                staleCutoffEpochSecond,
                org.springframework.data.domain.PageRequest.of(0, Math.min(Math.max(batchSize, 1), 5000))
        );

        java.util.List<String> keyHashes = slice.getContent();
        if (keyHashes.isEmpty()) {
            return 0;
        }

        int deleted = repository.deleteByKeyHashes(keyHashes);
        log.debug("Purged {} stale rate limit records with cutoff <= {}", deleted, staleCutoffEpochSecond);
        return deleted;
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest unavailable", e);
        }
    }
}

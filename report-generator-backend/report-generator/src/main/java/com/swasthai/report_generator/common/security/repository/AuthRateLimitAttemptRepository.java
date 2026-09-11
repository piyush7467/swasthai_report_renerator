package com.swasthai.report_generator.common.security.repository;

import com.swasthai.report_generator.common.security.entity.AuthRateLimitAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRateLimitAttemptRepository extends JpaRepository<AuthRateLimitAttempt, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuthRateLimitAttempt a WHERE a.keyHash = :keyHash")
    Optional<AuthRateLimitAttempt> findByKeyHashForUpdate(@Param("keyHash") String keyHash);

    @Query("SELECT a.keyHash FROM AuthRateLimitAttempt a " +
           "WHERE a.lockedUntilEpochSecond <= :cutoffEpochSecond " +
           "AND a.windowStartEpochSecond <= :cutoffEpochSecond")
    org.springframework.data.domain.Slice<String> findStaleKeyHashes(
            @Param("cutoffEpochSecond") long cutoffEpochSecond,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM AuthRateLimitAttempt a WHERE a.keyHash IN :keyHashes")
    int deleteByKeyHashes(@Param("keyHashes") java.util.Collection<String> keyHashes);
}

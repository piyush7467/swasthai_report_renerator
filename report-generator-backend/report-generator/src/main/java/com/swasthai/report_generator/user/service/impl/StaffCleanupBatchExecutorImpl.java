package com.swasthai.report_generator.user.service.impl;

import com.swasthai.report_generator.auth.repository.PasswordResetTokenRepository;
import com.swasthai.report_generator.auth.repository.RefreshTokenRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.StaffCleanupBatchExecutor;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffCleanupBatchExecutorImpl implements StaffCleanupBatchExecutor {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int cleanupBatch(Instant cutoff, int batchSize) {
        Slice<User> slice = userRepository.findEligibleForCleanup(
                Role.LAB_STAFF,
                UserStatus.INACTIVE,
                cutoff,
                PageRequest.of(0, batchSize)
        );

        List<User> batch = slice.getContent();
        if (batch.isEmpty()) {
            return 0;
        }

        for (User user : batch) {
            // Remove user's auth tokens prior to user deletion
            refreshTokenRepository.deleteByUser_Id(user.getId());
            passwordResetTokenRepository.deleteByUser_Id(user.getId());

            // Delete the staff member.
            // Historical reports, shares, audit logs and patient references are preserved with ON DELETE SET NULL.
            userRepository.delete(user);
        }

        userRepository.flush();
        entityManager.clear();

        log.info("Permanently purged batch of {} eligible inactive staff accounts", batch.size());
        return batch.size();
    }
}

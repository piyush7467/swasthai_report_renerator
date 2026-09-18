package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.common.exception.RateLimitExceededException;
import com.swasthai.report_generator.common.security.RateLimiterService;
import com.swasthai.report_generator.report.config.ReportVerificationRateLimitProperties;
import com.swasthai.report_generator.report.dto.response.PublicReportVerificationResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.PublicReportVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PublicReportVerificationServiceImpl implements PublicReportVerificationService {

    private static final String GENERIC_FAILURE_MESSAGE = "Report could not be verified.";
    private static final int MAX_REF_ID_LENGTH = 50;
    private static final int MIN_REF_ID_LENGTH = 3;

    private static final Pattern VALID_REF_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{3,50}$");

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ReportRepository reportRepository;
    private final RateLimiterService rateLimiterService;
    private final ReportVerificationRateLimitProperties properties;

    @Autowired
    public PublicReportVerificationServiceImpl(
            ReportRepository reportRepository,
            RateLimiterService rateLimiterService,
            @Autowired(required = false) ReportVerificationRateLimitProperties properties
    ) {
        this.reportRepository = reportRepository;
        this.rateLimiterService = rateLimiterService;
        this.properties = properties != null ? properties : new ReportVerificationRateLimitProperties();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicReportVerificationResponse verifyReport(String reportRefId, String clientIp) {
        // 1. Rate Limiting based on client network identifier
        enforceRateLimit(clientIp);

        // 2. Input validation and sanitization
        if (!isValidRefId(reportRefId)) {
            return PublicReportVerificationResponse.invalid(
                    reportRefId != null ? reportRefId.trim() : "",
                    GENERIC_FAILURE_MESSAGE
            );
        }

        String normalizedRefId = reportRefId.trim();

        // 3. Database query for finalized, non-deleted report
        Optional<Report> reportOptional =
                reportRepository.findByRefIdAndStatusAndDeletedAtIsNull(
                        normalizedRefId,
                        ReportStatus.FINALIZED
                );

        if (reportOptional.isEmpty()) {
            return PublicReportVerificationResponse.invalid(
                    normalizedRefId,
                    GENERIC_FAILURE_MESSAGE
            );
        }

        Report report = reportOptional.get();

        // 4. Return minimal sanitized response with historical snapshots
        return PublicReportVerificationResponse.valid(
                report.getRefId(),
                report.getOrganizationName(),
                report.getStatus().name(),
                report.getFinalizedAt()
        );
    }

    private void enforceRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        String ipKey = "verify:ip:" + clientIp.trim();
        int maxAttempts = properties.getMaxAttempts();
        long windowSeconds = properties.getWindowSeconds();
        long lockSeconds = properties.getLockSeconds();

        if (!rateLimiterService.isAllowed(ipKey, maxAttempts, windowSeconds, lockSeconds)) {
            log.warn("Public verification rate limit exceeded for network client");
            throw new RateLimitExceededException("Too many verification requests. Please try again later.");
        }
    }

    private boolean isValidRefId(String refId) {
        if (refId == null || refId.isBlank()) {
            return false;
        }

        String trimmed = refId.trim();

        if (trimmed.length() < MIN_REF_ID_LENGTH || trimmed.length() > MAX_REF_ID_LENGTH) {
            return false;
        }

        // Path traversal protection
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            return false;
        }

        // Reject UUIDs as alternative identifiers
        if (UUID_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // Whitelist alphanumeric, hyphen, and underscore characters only
        return VALID_REF_ID_PATTERN.matcher(trimmed).matches();
    }
}

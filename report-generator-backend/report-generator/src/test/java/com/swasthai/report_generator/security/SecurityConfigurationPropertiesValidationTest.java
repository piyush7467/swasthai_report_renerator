package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.config.AuthRateLimitProperties;
import com.swasthai.report_generator.license.config.LicenseProperties;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationPropertiesValidationTest {

    @Test
    @DisplayName("SEC-DATA-001: ReportRetentionProperties rejects zero or negative deleteAfterDays")
    void testRetentionProperties_RejectsZeroOrNegativeDeleteAfterDays() {
        ReportRetentionProperties props = new ReportRetentionProperties();
        props.setDeleteAfterDays(0);
        props.setPurgeAfterDays(10);
        props.setPurgeBatchSize(500);
        props.setBulkBatchSize(500);
        props.setPurgeEnabled(true);
        props.setPurgeCron("0 0 * * * *");
        props.setTimeZone("Asia/Kolkata");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delete-after-days must be greater than zero");

        props.setDeleteAfterDays(-5);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delete-after-days must be greater than zero");
    }

    @Test
    @DisplayName("SEC-DATA-001: ReportRetentionProperties rejects purgeAfterDays <= deleteAfterDays")
    void testRetentionProperties_RejectsPurgeLteDeleteAfterDays() {
        ReportRetentionProperties props = new ReportRetentionProperties();
        props.setDeleteAfterDays(10);
        props.setPurgeAfterDays(10); // Equal -> must fail
        props.setPurgeBatchSize(500);
        props.setBulkBatchSize(500);
        props.setPurgeEnabled(true);
        props.setPurgeCron("0 0 * * * *");
        props.setTimeZone("Asia/Kolkata");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly greater than delete-after-days");

        props.setPurgeAfterDays(5); // Less than -> must fail
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly greater than delete-after-days");
    }

    @Test
    @DisplayName("SEC-DATA-001: ReportRetentionProperties rejects excessive batch sizes")
    void testRetentionProperties_RejectsExcessiveBatchSizes() {
        ReportRetentionProperties props = new ReportRetentionProperties();
        props.setDeleteAfterDays(5);
        props.setPurgeAfterDays(10);
        props.setPurgeBatchSize(10000); // Exceeds 5000 limit
        props.setBulkBatchSize(500);
        props.setPurgeEnabled(true);
        props.setPurgeCron("0 0 * * * *");
        props.setTimeZone("Asia/Kolkata");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purge-batch-size must be between 1 and 5000");

        props.setPurgeBatchSize(500);
        props.setBulkBatchSize(5000); // Exceeds 2000 limit
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bulk-batch-size must be between 1 and 2000");
    }

    @Test
    @DisplayName("SEC-DATA-001: ReportRetentionProperties accepts valid safe configuration")
    void testRetentionProperties_AcceptsValidConfiguration() {
        ReportRetentionProperties props = new ReportRetentionProperties();
        props.setDeleteAfterDays(5);
        props.setPurgeAfterDays(10);
        props.setPurgeBatchSize(500);
        props.setBulkBatchSize(500);
        props.setPurgeEnabled(true);
        props.setPurgeCron("0 0 * * * *");
        props.setTimeZone("Asia/Kolkata");

        assertThatCode(props::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEC-LIC-001: LicenseProperties rejects zero or negative duration days")
    void testLicenseProperties_RejectsZeroOrNegative() {
        LicenseProperties props = new LicenseProperties();
        props.setDurationDays(0L);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("license.duration-days must be a positive integer");

        props.setDurationDays(-10L);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("license.duration-days must be a positive integer");

        props.setDurationDays(365L);
        assertThatCode(props::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEC-AUTH-001: AuthRateLimitProperties rejects zero or negative values")
    void testAuthRateLimitProperties_RejectsZeroOrNegative() {
        AuthRateLimitProperties props = new AuthRateLimitProperties();
        props.setMaxAttempts(0);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("security.rate-limit.max-attempts must be positive");

        props.setMaxAttempts(5);
        props.setWindowSeconds(-1L);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("security.rate-limit.window-seconds must be positive");

        props.setWindowSeconds(60L);
        props.setLockSeconds(0L);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("security.rate-limit.lock-seconds must be positive");

        props.setLockSeconds(300L);
        assertThatCode(props::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEC-AUTH-001: AuthRateLimitProperties validates cleanup configuration parameters")
    void testAuthRateLimitProperties_ValidatesCleanupParameters() {
        AuthRateLimitProperties props = new AuthRateLimitProperties();
        props.setCleanupBatchSize(0);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cleanup-batch-size must be between 1 and 5000");

        props.setCleanupBatchSize(6000);
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cleanup-batch-size must be between 1 and 5000");

        props.setCleanupBatchSize(500);
        props.setCleanupStaleAfterSeconds(30L); // below 60s
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cleanup-stale-after-seconds must be at least 60 seconds");

        props.setCleanupStaleAfterSeconds(86400L);
        props.setCleanupCron("invalid-cron-expr");
        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid security.rate-limit.cleanup-cron expression");

        props.setCleanupCron("0 0 * * * *");
        assertThatCode(props::validate).doesNotThrowAnyException();
    }
}
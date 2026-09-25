package com.swasthai.report_generator.license.service;

import com.swasthai.report_generator.common.exception.ConflictException;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LicenseGuard {

    private final LicenseService licenseService;
    private final LicenseRepository licenseRepository;
    private final ReportRepository reportRepository;

    /**
     * Must be called before creation of a billable report.
     *
     * This method intentionally accepts only the internal
     * organization UUID resolved by the authenticated user.
     *
     * It enforces:
     * 1. The organization has an active, non-expired license.
     * 2. Daily report generation limit (if configured > 0).
     * 3. Monthly report generation limit (if configured > 0).
     */
    public void requireReportCreationAllowed(
            UUID organizationId
    ) {
        licenseService.requireActiveLicenseForOrganization(organizationId);

        License license = licenseRepository
                .findWithPlanByOrganizationId(organizationId)
                .orElse(null);

        if (license == null || license.getPlan() == null) {
            return;
        }

        Plan plan = license.getPlan();

        // 1. Daily report quota check
        Integer maxDaily = plan.getMaxReportsPerDay();
        if (maxDaily != null && maxDaily > 0) {
            Instant startOfDay = LocalDate.now(ZoneOffset.UTC)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            long dailyReports = reportRepository.countByOrganization_IdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(
                    organizationId,
                    startOfDay
            );
            if (dailyReports >= maxDaily) {
                throw new ConflictException(
                        "Daily report generation limit reached for your organization ("
                                + maxDaily + " reports max). Please upgrade your subscription plan or try again tomorrow."
                );
            }
        }

        // 2. Monthly report quota check
        Integer maxMonthly = plan.getMaxReportsPerMonth();
        if (maxMonthly != null && maxMonthly > 0) {
            Instant startOfMonth = YearMonth.now(ZoneOffset.UTC)
                    .atDay(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            long monthlyReports = reportRepository.countByOrganization_IdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(
                    organizationId,
                    startOfMonth
            );
            if (monthlyReports >= maxMonthly) {
                throw new ConflictException(
                        "Monthly report generation limit reached for your organization ("
                                + maxMonthly + " reports max). Please upgrade your subscription plan to generate more reports."
                );
            }
        }
    }

    /**
     * Must be called before modifying, calculating, or finalizing a clinical report.
     * Enforces that an active, non-expired license is held by the organization.
     */
    public void requireActiveLicenseForOrganization(
            UUID organizationId
    ) {
        licenseService.requireActiveLicenseForOrganization(organizationId);
    }
}
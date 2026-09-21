package com.swasthai.report_generator.admin.analytics.service.impl;

import com.swasthai.report_generator.admin.analytics.dto.AdminOverviewStatsResponse;
import com.swasthai.report_generator.admin.analytics.dto.OrganizationReportActivityResponse;
import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.admin.analytics.dto.TestUsageStatsResponse;
import com.swasthai.report_generator.admin.analytics.repository.AdminAnalyticsRepository;
import com.swasthai.report_generator.admin.analytics.repository.OrganizationActivityProjection;
import com.swasthai.report_generator.admin.analytics.repository.ReportTrendProjection;
import com.swasthai.report_generator.admin.analytics.repository.TestUsageProjection;
import com.swasthai.report_generator.admin.analytics.service.AdminAnalyticsService;
import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.response.PageResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final AdminAnalyticsRepository adminAnalyticsRepository;
    private final OrganizationRepository organizationRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    @Value("${report.retention.time-zone:Asia/Kolkata}")
    private String timeZone;

    private ZoneId getResolvedZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (Exception e) {
            log.warn("Failed to parse timeZone: {}, falling back to UTC", timeZone);
            return ZoneOffset.UTC;
        }
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminOverviewStatsResponse getOverviewStats() {
        ZoneId zoneId = getResolvedZoneId();
        LocalDate today = LocalDate.now(zoneId);
        Instant startOfDay = today.atStartOfDay(zoneId).toInstant();
        Instant startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay(zoneId).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        long totalReports = adminAnalyticsRepository.countActiveReports();
        long reportsToday = adminAnalyticsRepository.countActiveReportsCreatedSince(startOfDay);
        long reportsThisWeek = adminAnalyticsRepository.countActiveReportsCreatedSince(startOfWeek);
        long reportsThisMonth = adminAnalyticsRepository.countActiveReportsCreatedSince(startOfMonth);

        long draftReports = adminAnalyticsRepository.countActiveReportsByStatus(ReportStatus.DRAFT);
        long calculatedReports = adminAnalyticsRepository.countActiveReportsByStatus(ReportStatus.CALCULATED);
        long finalizedReports = adminAnalyticsRepository.countActiveReportsByStatus(ReportStatus.FINALIZED);

        long deletedReportsToday = adminAnalyticsRepository.countDeletedReportsSince(startOfDay);
        long deletedReportsThisMonth = adminAnalyticsRepository.countDeletedReportsSince(startOfMonth);
        long totalDeletedReports = adminAnalyticsRepository.countTotalDeletedReports();

        long activeOrgs = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
        long orgsWithReports = adminAnalyticsRepository.countOrganizationsWithActiveReports();

        long breakGlass30d = securityAuditLogRepository.countByActionAndCreatedAtAfter(
                "BREAK_GLASS_REPORT_ACCESS",
                thirtyDaysAgo
        );

        return AdminOverviewStatsResponse.builder()
                .totalReports(totalReports)
                .reportsToday(reportsToday)
                .reportsThisWeek(reportsThisWeek)
                .reportsThisMonth(reportsThisMonth)
                .draftReports(draftReports)
                .calculatedReports(calculatedReports)
                .finalizedReports(finalizedReports)
                .deletedReportsToday(deletedReportsToday)
                .deletedReportsThisMonth(deletedReportsThisMonth)
                .totalDeletedReports(totalDeletedReports)
                .activeOrganizationsCount(activeOrgs)
                .organizationsWithReportsCount(orgsWithReports)
                .breakGlassAccessCount30Days(breakGlass30d)
                .build();
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<ReportTrendPointResponse> getReportTrend(
            Integer days,
            LocalDate from,
            LocalDate to,
            String organizationRefId) {

        ZoneId zoneId = getResolvedZoneId();
        LocalDate today = LocalDate.now(zoneId);

        LocalDate startDate;
        LocalDate endDate;

        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new BadRequestException("Start date cannot be after end date");
            }
            if (ChronoUnit.DAYS.between(from, to) > 90) {
                throw new BadRequestException("Date range cannot exceed 90 days");
            }
            startDate = from;
            endDate = to;
        } else {
            int rangeDays = (days != null && days > 0) ? Math.min(days, 90) : 30;
            endDate = today;
            startDate = today.minusDays(rangeDays - 1);
        }

        Instant fromInstant = startDate.atStartOfDay(zoneId).toInstant();
        Instant toInstant = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        UUID organizationId = null;
        if (organizationRefId != null && !organizationRefId.isBlank()) {
            Organization org = organizationRepository.findByRefId(organizationRefId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with refId: " + organizationRefId));
            organizationId = org.getId();
        }

        List<ReportTrendProjection> projections = adminAnalyticsRepository.findReportTrend(
                fromInstant,
                toInstant,
                timeZone,
                organizationId
        );

        Map<String, ReportTrendProjection> projectionMap = new HashMap<>();
        if (projections != null) {
            for (ReportTrendProjection p : projections) {
                if (p.getReportDate() != null) {
                    projectionMap.put(p.getReportDate().toString(), p);
                }
            }
        }

        List<ReportTrendPointResponse> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.toString();
            ReportTrendProjection p = projectionMap.get(dateKey);

            result.add(ReportTrendPointResponse.builder()
                    .date(dateKey)
                    .totalCount(p != null && p.getTotalCount() != null ? p.getTotalCount() : 0L)
                    .finalizedCount(p != null && p.getFinalizedCount() != null ? p.getFinalizedCount() : 0L)
                    .draftCount(p != null && p.getDraftCount() != null ? p.getDraftCount() : 0L)
                    .build());

            current = current.plusDays(1);
        }

        return result;
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<OrganizationReportActivityResponse> getOrganizationReportActivity(
            String search,
            int page,
            int size,
            String sort,
            String direction) {

        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 50);

        String safeSort = mapSafeSortColumn(sort);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(sortDirection, safeSort));

        ZoneId zoneId = getResolvedZoneId();
        LocalDate today = LocalDate.now(zoneId);
        Instant startOfDay = today.atStartOfDay(zoneId).toInstant();
        Instant startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay(zoneId).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();

        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<OrganizationActivityProjection> resultPage = adminAnalyticsRepository.findOrganizationReportActivity(
                startOfDay,
                startOfWeek,
                startOfMonth,
                trimmedSearch,
                pageable
        );

        List<OrganizationReportActivityResponse> content = resultPage.getContent().stream()
                .map(this::mapToOrganizationReportActivityResponse)
                .toList();

        return PageResponse.<OrganizationReportActivityResponse>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<TestUsageStatsResponse> getTestUsage(Integer limit, String organizationRefId) {
        int validLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

        UUID organizationId = null;
        if (organizationRefId != null && !organizationRefId.isBlank()) {
            Organization org = organizationRepository.findByRefId(organizationRefId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with refId: " + organizationRefId));
            organizationId = org.getId();
        }

        List<TestUsageProjection> projections = adminAnalyticsRepository.findTopTestUsage(validLimit, organizationId);
        if (projections == null) {
            return Collections.emptyList();
        }

        return projections.stream()
                .map(p -> TestUsageStatsResponse.builder()
                        .testRefId(p.getTestRefId())
                        .testCode(p.getTestCode())
                        .testName(p.getTestName())
                        .testShortName(p.getTestShortName())
                        .categoryName(p.getCategoryName() != null ? p.getCategoryName() : "Uncategorized")
                        .usageCount(p.getUsageCount() != null ? p.getUsageCount() : 0L)
                        .build())
                .toList();
    }

    private String mapSafeSortColumn(String sort) {
        if (sort == null || sort.isBlank()) {
            return "totalReports";
        }
        return switch (sort.trim().toLowerCase()) {
            case "reports_today", "reportstoday" -> "reportsToday";
            case "reports_this_week", "reportsthisweek" -> "reportsThisWeek";
            case "reports_this_month", "reportsthismonth" -> "reportsThisMonth";
            case "draft_reports", "draftreports" -> "draftReports";
            case "calculated_reports", "calculatedreports" -> "calculatedReports";
            case "finalized_reports", "finalizedreports" -> "finalizedReports";
            case "last_report_created_at", "lastreportcreatedat" -> "lastReportCreatedAt";
            case "name", "organizationname" -> "o.name";
            case "code", "organizationcode" -> "o.code";
            default -> "totalReports";
        };
    }

    private OrganizationReportActivityResponse mapToOrganizationReportActivityResponse(OrganizationActivityProjection p) {
        OrganizationStatus status = null;
        if (p.getOrganizationStatus() != null) {
            try {
                status = OrganizationStatus.valueOf(p.getOrganizationStatus());
            } catch (Exception ignored) {
            }
        }

        return OrganizationReportActivityResponse.builder()
                .organizationRefId(p.getOrganizationRefId())
                .organizationName(p.getOrganizationName())
                .organizationCode(p.getOrganizationCode())
                .organizationStatus(status)
                .totalReports(p.getTotalReports() != null ? p.getTotalReports() : 0L)
                .reportsToday(p.getReportsToday() != null ? p.getReportsToday() : 0L)
                .reportsThisWeek(p.getReportsThisWeek() != null ? p.getReportsThisWeek() : 0L)
                .reportsThisMonth(p.getReportsThisMonth() != null ? p.getReportsThisMonth() : 0L)
                .draftReports(p.getDraftReports() != null ? p.getDraftReports() : 0L)
                .calculatedReports(p.getCalculatedReports() != null ? p.getCalculatedReports() : 0L)
                .finalizedReports(p.getFinalizedReports() != null ? p.getFinalizedReports() : 0L)
                .lastReportCreatedAt(p.getLastReportCreatedAt())
                .build();
    }
}

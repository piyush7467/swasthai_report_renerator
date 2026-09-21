package com.swasthai.report_generator.admin.analytics.repository;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdminAnalyticsRepository extends JpaRepository<Report, UUID> {

    @Query("SELECT COUNT(r) FROM Report r WHERE r.deletedAt IS NULL")
    long countActiveReports();

    @Query("SELECT COUNT(r) FROM Report r WHERE r.deletedAt IS NULL AND r.createdAt >= :startInstant")
    long countActiveReportsCreatedSince(@Param("startInstant") Instant startInstant);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.deletedAt IS NULL AND r.status = :status")
    long countActiveReportsByStatus(@Param("status") ReportStatus status);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.deletedAt IS NOT NULL AND r.deletedAt >= :startInstant")
    long countDeletedReportsSince(@Param("startInstant") Instant startInstant);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.deletedAt IS NOT NULL")
    long countTotalDeletedReports();

    @Query("SELECT COUNT(DISTINCT r.organization.id) FROM Report r WHERE r.deletedAt IS NULL")
    long countOrganizationsWithActiveReports();

    @Query(value = """
        SELECT
            CAST(r.created_at AT TIME ZONE :timeZone AS DATE) AS reportDate,
            COUNT(*) AS totalCount,
            COUNT(CASE WHEN r.status = 'FINALIZED' THEN 1 END) AS finalizedCount,
            COUNT(CASE WHEN r.status = 'DRAFT' THEN 1 END) AS draftCount
        FROM reports r
        WHERE r.deleted_at IS NULL
          AND r.created_at >= :fromInstant
          AND r.created_at < :toInstant
          AND (:organizationId IS NULL OR r.organization_id = :organizationId)
        GROUP BY reportDate
        ORDER BY reportDate ASC
        """, nativeQuery = true)
    List<ReportTrendProjection> findReportTrend(
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant,
            @Param("timeZone") String timeZone,
            @Param("organizationId") UUID organizationId
    );

    @Query(value = """
        SELECT
            o.ref_id AS organizationRefId,
            o.name AS organizationName,
            o.code AS organizationCode,
            o.status AS organizationStatus,
            COUNT(r.id) AS totalReports,
            COUNT(CASE WHEN r.created_at >= :startOfDay THEN 1 END) AS reportsToday,
            COUNT(CASE WHEN r.created_at >= :startOfWeek THEN 1 END) AS reportsThisWeek,
            COUNT(CASE WHEN r.created_at >= :startOfMonth THEN 1 END) AS reportsThisMonth,
            COUNT(CASE WHEN r.status = 'DRAFT' THEN 1 END) AS draftReports,
            COUNT(CASE WHEN r.status = 'CALCULATED' THEN 1 END) AS calculatedReports,
            COUNT(CASE WHEN r.status = 'FINALIZED' THEN 1 END) AS finalizedReports,
            MAX(r.created_at) AS lastReportCreatedAt
        FROM organizations o
        LEFT JOIN reports r ON r.organization_id = o.id AND r.deleted_at IS NULL
        WHERE (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.code) LIKE LOWER(CONCAT('%', :search, '%')))
        GROUP BY o.id, o.ref_id, o.name, o.code, o.status
        """,
        countQuery = """
        SELECT COUNT(o.id)
        FROM organizations o
        WHERE (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.code) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        nativeQuery = true)
    Page<OrganizationActivityProjection> findOrganizationReportActivity(
            @Param("startOfDay") Instant startOfDay,
            @Param("startOfWeek") Instant startOfWeek,
            @Param("startOfMonth") Instant startOfMonth,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = """
        SELECT
            t.ref_id AS testRefId,
            t.code AS testCode,
            t.name AS testName,
            t.short_name AS testShortName,
            c.name AS categoryName,
            COUNT(rtr.id) AS usageCount
        FROM report_test_results rtr
        JOIN reports r ON r.id = rtr.report_id AND r.deleted_at IS NULL
        JOIN tests t ON t.id = rtr.test_id
        LEFT JOIN test_categories c ON c.id = t.category_id
        WHERE (:organizationId IS NULL OR r.organization_id = :organizationId)
        GROUP BY t.ref_id, t.code, t.name, t.short_name, c.name
        ORDER BY usageCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TestUsageProjection> findTopTestUsage(
            @Param("limit") int limit,
            @Param("organizationId") UUID organizationId
    );
}

package com.swasthai.report_generator.report.repository;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository
        extends JpaRepository<Report, UUID>,
        JpaSpecificationExecutor<Report> {

    Optional<Report> findByRefId(String refId);

    boolean existsByRefId(String refId);

    Optional<Report> findByRefIdAndOrganization_Id(
            String refId,
            UUID organizationId
    );

    Page<Report> findAllByOrganization_Id(
            UUID organizationId,
            Pageable pageable
    );

    Page<Report> findAllByOrganization_IdAndStatus(
            UUID organizationId,
            ReportStatus status,
            Pageable pageable
    );

    Page<Report> findAllByOrganization_IdAndPatientRefId(
            UUID organizationId,
            String patientRefId,
            Pageable pageable
    );

    Optional<Report> findByRefIdAndDeletedAtIsNull(
            String refId
    );

    Optional<Report> findByRefIdAndStatusAndDeletedAtIsNull(
            String refId,
            ReportStatus status
    );

    Optional<Report> findByRefIdAndOrganization_IdAndDeletedAtIsNull(
            String refId,
            UUID organizationId
    );

    Page<Report> findAllByOrganization_IdAndDeletedAtIsNull(
            UUID organizationId,
            Pageable pageable
    );

    Page<Report> findAllByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            ReportStatus status,
            Pageable pageable
    );

    Page<Report> findAllByOrganization_IdAndPatientRefIdAndDeletedAtIsNull(
            UUID organizationId,
            String patientRefId,
            Pageable pageable
    );

    List<Report> findAllByOrganization_IdAndRefIdInAndDeletedAtIsNull(
            UUID organizationId,
            Collection<String> reportRefIds
    );

    @Query("""
        SELECT r
        FROM Report r
        WHERE r.organization.id = :organizationId
          AND r.deletedAt IS NULL
          AND r.createdAt >= :startInstant
          AND r.createdAt < :effectiveEndInstant
        ORDER BY r.createdAt ASC
        """)
    Slice<Report> findEligibleForDateRangeDeletion(
            @Param("organizationId") UUID organizationId,
            @Param("startInstant") Instant startInstant,
            @Param("effectiveEndInstant") Instant effectiveEndInstant,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(r)
        FROM Report r
        WHERE r.organization.id = :organizationId
          AND r.deletedAt IS NULL
          AND r.createdAt >= :effectiveEndInstant
          AND r.createdAt < :endExclusiveInstant
        """)
    long countYoungSkippedReports(
            @Param("organizationId") UUID organizationId,
            @Param("effectiveEndInstant") Instant effectiveEndInstant,
            @Param("endExclusiveInstant") Instant endExclusiveInstant
    );

    @Query("""
        SELECT r
        FROM Report r
        WHERE r.deletedAt IS NOT NULL
          AND r.deletedAt < :purgeCutoff
        ORDER BY r.deletedAt ASC
        """)
    Slice<Report> findEligibleForPurge(
            @Param("purgeCutoff") Instant purgeCutoff,
            Pageable pageable
    );
}
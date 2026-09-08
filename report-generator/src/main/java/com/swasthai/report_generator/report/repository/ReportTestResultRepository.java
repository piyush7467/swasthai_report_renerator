package com.swasthai.report_generator.report.repository;

import com.swasthai.report_generator.report.entity.ReportTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportTestResultRepository
        extends JpaRepository<ReportTestResult, UUID> {

    Optional<ReportTestResult> findByRefId(String refId);

    boolean existsByRefId(String refId);

    Optional<ReportTestResult> findByReport_IdAndTest_Id(
            UUID reportId,
            UUID testId
    );

    boolean existsByReport_IdAndTest_Id(
            UUID reportId,
            UUID testId
    );

    Optional<ReportTestResult> findByRefIdAndReport_Organization_Id(
            String refId,
            UUID organizationId
    );

    Optional<ReportTestResult> findByRefIdAndReport_RefId(
            String refId,
            String reportRefId
    );

    List<ReportTestResult> findAllByReport_IdOrderByDisplayOrderAsc(
            UUID reportId
    );
}
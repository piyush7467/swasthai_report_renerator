package com.swasthai.report_generator.report.repository;

import com.swasthai.report_generator.report.entity.ReportShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportShareRepository extends JpaRepository<ReportShare, UUID> {

    Optional<ReportShare> findByShareToken(String shareToken);

    Optional<ReportShare> findByShareTokenAndRevokedFalse(String shareToken);

    List<ReportShare> findAllByReport_IdOrderByCreatedAtDesc(UUID reportId);

    List<ReportShare> findAllByReportRefIdOrderByCreatedAtDesc(String reportRefId);
}

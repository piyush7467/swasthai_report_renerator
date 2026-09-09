package com.swasthai.report_generator.report.repository;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository
        extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {

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
}
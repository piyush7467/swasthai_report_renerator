package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.PatientTestResult;
import com.swasthai.report_generator.test.entity.PatientTestResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientTestResultRepository
        extends JpaRepository<PatientTestResult, UUID> {

    Optional<PatientTestResult> findByRefId(String refId);

    boolean existsByRefId(String refId);

    Page<PatientTestResult> findAllByOrganization_Id(
            UUID organizationId,
            Pageable pageable
    );

    Page<PatientTestResult> findAllByOrganization_IdAndStatus(
            UUID organizationId,
            PatientTestResultStatus status,
            Pageable pageable
    );

    Page<PatientTestResult> findAllByOrganization_IdAndPatientRefId(
            UUID organizationId,
            String patientRefId,
            Pageable pageable
    );

    Page<PatientTestResult> findAllByOrganization_IdAndTest_Id(
            UUID organizationId,
            UUID testId,
            Pageable pageable
    );

    Optional<PatientTestResult> findByIdAndOrganization_Id(
            UUID id,
            UUID organizationId
    );

    Optional<PatientTestResult> findByRefIdAndOrganization_Id(
            String refId,
            UUID organizationId
    );
}
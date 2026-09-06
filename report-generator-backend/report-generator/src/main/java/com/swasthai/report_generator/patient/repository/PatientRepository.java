package com.swasthai.report_generator.patient.repository;

import com.swasthai.report_generator.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository
        extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByRefId(String refId);

    Optional<Patient> findByRefIdAndOrganization_Id(
            String refId,
            UUID organizationId
    );

    Optional<Patient> findByPatientCodeAndOrganization_Id(
            String patientCode,
            UUID organizationId
    );

    boolean existsByPatientCodeAndOrganization_Id(
            String patientCode,
            UUID organizationId
    );

    boolean existsByRefId(String refId);

    boolean existsByPhoneAndOrganization_Id(
            String phone,
            UUID organizationId
    );

    List<Patient> findAllByOrganization_IdOrderByCreatedAtDesc(
            UUID organizationId
    );

    org.springframework.data.domain.Page<Patient> findAllByOrganization_Id(
            UUID organizationId,
            org.springframework.data.domain.Pageable pageable
    );
}
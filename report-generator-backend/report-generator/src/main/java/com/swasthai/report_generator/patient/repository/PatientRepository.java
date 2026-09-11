package com.swasthai.report_generator.patient.repository;

import com.swasthai.report_generator.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /*
     * ============================================================
     * BASIC LOOKUPS
     * ============================================================
     */

    /**
     * Finds a patient by public reference ID.
     *
     * This lookup intentionally includes soft-deleted patients.
     * It is required for restore operations.
     */
    Optional<Patient> findByRefId(String refId);

    /**
     * Active patient lookup by public reference ID.
     */
    Optional<Patient> findByRefIdAndDeletedAtIsNull(String refId);

    /**
     * Tenant-scoped active patient lookup.
     */
    Optional<Patient> findByRefIdAndOrganization_IdAndDeletedAtIsNull(
            String refId,
            UUID organizationId
    );

    /**
     * Tenant-scoped active patient lookup by patient code.
     */
    Optional<Patient> findByPatientCodeAndOrganization_IdAndDeletedAtIsNull(
            String patientCode,
            UUID organizationId
    );

    /*
     * ============================================================
     * EXISTENCE CHECKS
     * ============================================================
     */

    boolean existsByPatientCodeAndOrganization_Id(
            String patientCode,
            UUID organizationId
    );

    boolean existsByRefId(String refId);

    boolean existsByPhoneAndOrganization_IdAndDeletedAtIsNull(
            String phone,
            UUID organizationId
    );

    /*
     * ============================================================
     * ACTIVE PATIENT LIST
     * ============================================================
     */

    Page<Patient> findAllByOrganization_IdAndDeletedAtIsNull(
            UUID organizationId,
            Pageable pageable
    );

    /*
     * ============================================================
     * ACTIVE PATIENT SEARCH
     * ============================================================
     */

    @Query("""
            SELECT p
            FROM Patient p
            WHERE p.organization.id = :organizationId
              AND p.deletedAt IS NULL
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Patient> searchActivePatients(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable
    );
}
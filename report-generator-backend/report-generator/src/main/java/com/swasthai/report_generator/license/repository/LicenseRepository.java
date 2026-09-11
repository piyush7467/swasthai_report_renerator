package com.swasthai.report_generator.license.repository;

import com.swasthai.report_generator.license.entity.License;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository
        extends JpaRepository<License, UUID> {

    Optional<License> findByRefId(String refId);

    Optional<License> findByOrganization_Id(UUID organizationId);

    /**
     * Used only by SUPER_ADMIN license mutations.
     *
     * Pessimistic locking prevents concurrent activation/renewal
     * of the same existing license.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l
            FROM License l
            WHERE l.organization.id = :organizationId
            """)
    Optional<License> findByOrganizationIdForUpdate(
            @Param("organizationId") UUID organizationId
    );

    @Query("""
            SELECT l
            FROM License l
            JOIN FETCH l.plan
            WHERE l.organization.id = :organizationId
            """)
    Optional<License> findWithPlanByOrganizationId(
            @Param("organizationId") UUID organizationId
    );
}
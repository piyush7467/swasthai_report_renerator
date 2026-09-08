package com.swasthai.report_generator.organization.repository;

import com.swasthai.report_generator.organization.entity.OrganizationSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationSequenceRepository
        extends JpaRepository<OrganizationSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sequence
            FROM OrganizationSequence sequence
            WHERE sequence.organization.id = :organizationId
            """)
    Optional<OrganizationSequence> findByOrganizationIdForUpdate(
            @Param("organizationId") UUID organizationId
    );
}
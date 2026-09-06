package com.swasthai.report_generator.organization.repository;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository
        extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByRefId(String refId);

    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    Optional<Organization> findByIdAndStatus(
            UUID id,
            OrganizationStatus status
    );
}
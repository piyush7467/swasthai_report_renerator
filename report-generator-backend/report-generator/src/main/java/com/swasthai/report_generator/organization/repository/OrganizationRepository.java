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

    boolean existsByCodeAndIdNot(
            String code,
            UUID id
    );

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            UUID id
    );

    Optional<Organization> findByIdAndStatus(
            UUID id,
            OrganizationStatus status
    );

    long countByStatus(OrganizationStatus status);

    java.util.List<Organization> findAllByRefIdIn(java.util.Collection<String> refIds);
}
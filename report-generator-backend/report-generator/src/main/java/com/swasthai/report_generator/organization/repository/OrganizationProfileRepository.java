package com.swasthai.report_generator.organization.repository;

import com.swasthai.report_generator.organization.entity.OrganizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationProfileRepository
        extends JpaRepository<OrganizationProfile, UUID> {

    Optional<OrganizationProfile> findByOrganization_Id(
            UUID organizationId
    );

    boolean existsByOrganization_Id(
            UUID organizationId
    );
}
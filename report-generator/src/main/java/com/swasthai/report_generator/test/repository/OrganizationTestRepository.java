package com.swasthai.report_generator.test.repository;

import com.swasthai.report_generator.test.entity.OrganizationTest;
import com.swasthai.report_generator.test.entity.OrganizationTestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationTestRepository
        extends JpaRepository<OrganizationTest, UUID> {

    // Public reference ID

    Optional<OrganizationTest> findByRefId(String refId);

    boolean existsByRefId(String refId);

    // Organization + Test

    Optional<OrganizationTest>
    findByOrganization_IdAndTest_Id(
            UUID organizationId,
            UUID testId
    );

    boolean existsByOrganization_IdAndTest_Id(
            UUID organizationId,
            UUID testId
    );

    boolean existsByOrganization_IdAndTest_IdAndStatus(
            UUID organizationId,
            UUID testId,
            OrganizationTestStatus status
    );

    // Organization + Test using public refIds

    Optional<OrganizationTest>
    findByOrganization_RefIdAndTest_RefId(
            String organizationRefId,
            String testRefId
    );

    boolean existsByOrganization_RefIdAndTest_RefId(
            String organizationRefId,
            String testRefId
    );

    boolean existsByOrganization_RefIdAndTest_RefIdAndStatus(
            String organizationRefId,
            String testRefId,
            OrganizationTestStatus status
    );

    // Organization tests

    Page<OrganizationTest>
    findAllByOrganization_Id(
            UUID organizationId,
            Pageable pageable
    );

    Page<OrganizationTest>
    findAllByOrganization_IdAndStatus(
            UUID organizationId,
            OrganizationTestStatus status,
            Pageable pageable
    );

    Page<OrganizationTest>
    findAllByOrganization_RefId(
            String organizationRefId,
            Pageable pageable
    );

    Page<OrganizationTest>
    findAllByOrganization_RefIdAndStatus(
            String organizationRefId,
            OrganizationTestStatus status,
            Pageable pageable
    );

    // Test usage

    Page<OrganizationTest>
    findAllByTest_Id(
            UUID testId,
            Pageable pageable
    );

    Page<OrganizationTest>
    findAllByTest_RefId(
            String testRefId,
            Pageable pageable
    );
}
package com.swasthai.report_generator.license.repository;

import com.swasthai.report_generator.license.entity.PlanUpgradeRequest;
import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanUpgradeRequestRepository extends JpaRepository<PlanUpgradeRequest, UUID> {

    Optional<PlanUpgradeRequest> findByRefId(String refId);

    Optional<PlanUpgradeRequest> findByRefIdAndOrganization_Id(String refId, UUID organizationId);

    boolean existsByOrganization_IdAndRequestedPlan_IdAndStatus(
            UUID organizationId,
            UUID requestedPlanId,
            UpgradeRequestStatus status
    );

    Page<PlanUpgradeRequest> findAllByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<PlanUpgradeRequest> findAllByOrganization_IdAndStatus(
            UUID organizationId,
            UpgradeRequestStatus status,
            Pageable pageable
    );

    @Query("SELECT r FROM PlanUpgradeRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:orgId IS NULL OR r.organization.id = :orgId)")
    Page<PlanUpgradeRequest> findAllWithFilters(
            @Param("status") UpgradeRequestStatus status,
            @Param("orgId") UUID orgId,
            Pageable pageable
    );
}

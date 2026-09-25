package com.swasthai.report_generator.license.dto.response;

import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PlanUpgradeRequestResponse(
        String refId,
        String organizationRefId,
        String organizationName,
        String requestedByEmail,
        String currentPlanRefId,
        String currentPlanName,
        String requestedPlanRefId,
        String requestedPlanName,
        int currentActiveStaffCount,
        int requestedStaffCapacity,
        String reason,
        String contactName,
        String contactEmail,
        String contactPhone,
        String additionalMessage,
        UpgradeRequestStatus status,
        String reviewedByEmail,
        Instant reviewedAt,
        String adminNotes,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}

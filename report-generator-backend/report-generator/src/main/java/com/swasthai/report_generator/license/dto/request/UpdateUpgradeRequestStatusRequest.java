package com.swasthai.report_generator.license.dto.request;

import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUpgradeRequestStatusRequest(

        @NotNull(message = "Status is required.")
        UpgradeRequestStatus status,

        @Size(max = 1000, message = "Admin notes cannot exceed 1000 characters.")
        String adminNotes,

        @Size(max = 500, message = "Rejection reason cannot exceed 500 characters.")
        String rejectionReason
) {
}

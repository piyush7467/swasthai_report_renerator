package com.swasthai.report_generator.license.dto.response;

import com.swasthai.report_generator.license.entity.LicenseStatus;

import java.time.Instant;

public record LicenseResponse(

        String refId,

        String organizationRefId,

        String planRefId,

        String planCode,

        String planName,

        Integer maxLabStaff,

        Integer maxReportsPerMonth,

        Integer maxReportsPerDay,

        LicenseStatus status,

        Instant startedAt,

        Instant expiresAt,

        boolean currentlyUsable,

        Instant createdAt,

        Instant updatedAt
) {
}
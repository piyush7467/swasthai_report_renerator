package com.swasthai.report_generator.license.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AvailablePlanResponse(
        String refId,
        String code,
        String name,
        String description,
        BigDecimal annualPrice,
        String currency,
        int maxLabStaff,
        Integer maxReportsPerMonth,
        Integer maxReportsPerDay,
        boolean currentPlan,
        boolean upgrade
) {
}

package com.swasthai.report_generator.license.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PlanResponse(

        String refId,

        String code,

        String name,

        String description,

        BigDecimal annualPrice,

        String currency,

        boolean active,

        Instant createdAt,

        Instant updatedAt
) {
}
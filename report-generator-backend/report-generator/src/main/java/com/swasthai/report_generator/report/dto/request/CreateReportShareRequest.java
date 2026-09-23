package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateReportShareRequest(
        @NotBlank(message = "Share channel is required")
        @Pattern(regexp = "WHATSAPP|EMAIL|LINK|SYSTEM", message = "Channel must be WHATSAPP, EMAIL, LINK, or SYSTEM")
        String channel,

        String recipient,

        @Min(value = 1, message = "Expiration must be at least 1 day")
        @Max(value = 90, message = "Expiration cannot exceed 90 days")
        Integer expiresInDays
) {
}

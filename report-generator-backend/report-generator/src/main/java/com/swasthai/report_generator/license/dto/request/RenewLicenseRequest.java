package com.swasthai.report_generator.license.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenewLicenseRequest(

        @NotBlank
        String planRefId,

        @Size(max = 150)
        String paymentReference
) {
}
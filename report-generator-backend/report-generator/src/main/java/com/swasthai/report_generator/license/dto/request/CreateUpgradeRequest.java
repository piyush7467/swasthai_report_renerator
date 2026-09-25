package com.swasthai.report_generator.license.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUpgradeRequest(

        @NotBlank(message = "Requested plan is required.")
        String requestedPlanRefId,

        @Size(max = 500, message = "Reason cannot exceed 500 characters.")
        String reason,

        @NotBlank(message = "Contact name is required.")
        @Size(min = 2, max = 150, message = "Contact name must be between 2 and 150 characters.")
        String contactName,

        @NotBlank(message = "Contact email is required.")
        @Email(message = "A valid contact email is required.")
        @Size(max = 255, message = "Contact email cannot exceed 255 characters.")
        String contactEmail,

        @Size(max = 50, message = "Contact phone cannot exceed 50 characters.")
        String contactPhone,

        @Size(max = 1000, message = "Additional message cannot exceed 1000 characters.")
        String additionalMessage
) {
}

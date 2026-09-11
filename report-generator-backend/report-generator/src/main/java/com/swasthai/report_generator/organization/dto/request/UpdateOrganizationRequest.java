package com.swasthai.report_generator.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(
            max = 150,
            message = "Organization name must not exceed 150 characters"
    )
    private String name;

    @NotBlank(message = "Organization code is required")
    @Size(
            max = 50,
            message = "Organization code must not exceed 50 characters"
    )
    private String code;
}
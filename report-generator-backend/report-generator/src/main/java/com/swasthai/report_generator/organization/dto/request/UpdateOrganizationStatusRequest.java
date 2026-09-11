package com.swasthai.report_generator.organization.dto.request;

import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Organization status is required")
    private OrganizationStatus status;
}
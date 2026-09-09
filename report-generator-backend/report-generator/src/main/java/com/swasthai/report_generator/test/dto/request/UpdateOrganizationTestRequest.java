package com.swasthai.report_generator.test.dto.request;

import com.swasthai.report_generator.test.entity.OrganizationTestStatus;


import java.time.LocalDate;

public record UpdateOrganizationTestRequest(

        OrganizationTestStatus status,

        LocalDate effectiveFrom,

        LocalDate effectiveUntil
) {
}
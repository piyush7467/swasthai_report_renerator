package com.swasthai.report_generator.report.pdf;

import com.swasthai.report_generator.report.entity.ReportStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ReportPdfData(

        String reportRefId,

        ReportStatus status,

        Integer reportVersion,

        Instant createdAt,

        Instant finalizedAt,

        OrganizationPdfSnapshot organization,

        PatientPdfSnapshot patient,

        UserPdfSnapshot createdBy,

        UserPdfSnapshot finalizedBy,

        List<TestPdfItem> tests,

        String verificationUrl,

        Boolean includeOrganizationHeader
) {
}
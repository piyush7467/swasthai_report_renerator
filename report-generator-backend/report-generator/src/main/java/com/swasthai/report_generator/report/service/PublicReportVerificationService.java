package com.swasthai.report_generator.report.service;

import com.swasthai.report_generator.report.dto.response.PublicReportVerificationResponse;

public interface PublicReportVerificationService {

    /**
     * Verifies the authenticity of a finalized report using its public immutable refId.
     * Rate-limited based on client IP / network identifier.
     *
     * @param reportRefId immutable public report reference ID
     * @param clientIp peer socket client IP address
     * @return PublicReportVerificationResponse
     */
    PublicReportVerificationResponse verifyReport(String reportRefId, String clientIp);
}

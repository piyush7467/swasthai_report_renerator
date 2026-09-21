package com.swasthai.report_generator.admin.audit.service;

import com.swasthai.report_generator.admin.audit.dto.SecurityAuditLogResponse;
import com.swasthai.report_generator.common.response.PageResponse;

import java.time.LocalDate;

public interface AdminAuditService {

    PageResponse<SecurityAuditLogResponse> getSecurityAuditLogs(
            String action,
            String targetOrganizationRefId,
            Boolean success,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            String sort,
            String direction
    );
}

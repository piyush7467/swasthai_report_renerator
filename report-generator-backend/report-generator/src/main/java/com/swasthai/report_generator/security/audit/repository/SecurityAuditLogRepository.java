package com.swasthai.report_generator.security.audit.repository;

import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {

    Optional<SecurityAuditLog> findByRefId(String refId);

    List<SecurityAuditLog> findByTargetReportRefIdOrderByCreatedAtDesc(String targetReportRefId);

    List<SecurityAuditLog> findByTargetOrganizationRefIdOrderByCreatedAtDesc(String targetOrganizationRefId);
}

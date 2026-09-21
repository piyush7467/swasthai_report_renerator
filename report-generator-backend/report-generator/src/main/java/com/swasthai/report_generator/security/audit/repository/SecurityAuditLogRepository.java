package com.swasthai.report_generator.security.audit.repository;

import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository
        extends JpaRepository<SecurityAuditLog, UUID>,
        JpaSpecificationExecutor<SecurityAuditLog> {

    Optional<SecurityAuditLog> findByRefId(String refId);

    List<SecurityAuditLog> findByTargetReportRefIdOrderByCreatedAtDesc(String targetReportRefId);

    List<SecurityAuditLog> findByTargetOrganizationRefIdOrderByCreatedAtDesc(String targetOrganizationRefId);

    long countByCreatedAtAfter(Instant cutoff);

    long countByActionAndCreatedAtAfter(String action, Instant cutoff);
}


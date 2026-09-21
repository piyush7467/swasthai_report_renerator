package com.swasthai.report_generator.admin.audit.service.impl;

import com.swasthai.report_generator.admin.audit.dto.SecurityAuditLogResponse;
import com.swasthai.report_generator.admin.audit.service.AdminAuditService;
import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.response.PageResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAuditServiceImpl implements AdminAuditService {

    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${report.retention.time-zone:Asia/Kolkata}")
    private String timeZone;

    private ZoneId getResolvedZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (Exception e) {
            log.warn("Failed to parse timeZone: {}, falling back to UTC", timeZone);
            return ZoneOffset.UTC;
        }
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<SecurityAuditLogResponse> getSecurityAuditLogs(
            String action,
            String targetOrganizationRefId,
            Boolean success,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            String sort,
            String direction) {

        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);

        String safeSort = mapSafeSortColumn(sort);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(sortDirection, safeSort));

        ZoneId zoneId = getResolvedZoneId();

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        Instant startInstant = from != null ? from.atStartOfDay(zoneId).toInstant() : null;
        Instant endInstant = to != null ? to.plusDays(1).atStartOfDay(zoneId).toInstant() : null;

        Specification<SecurityAuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("action")),
                        action.trim().toLowerCase()
                ));
            }

            if (targetOrganizationRefId != null && !targetOrganizationRefId.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("targetOrganizationRefId"),
                        targetOrganizationRefId.trim()
                ));
            }

            if (success != null) {
                predicates.add(criteriaBuilder.equal(root.get("success"), success));
            }

            if (startInstant != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startInstant));
            }

            if (endInstant != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), endInstant));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<SecurityAuditLog> resultPage = securityAuditLogRepository.findAll(spec, pageable);

        Set<String> orgRefIds = resultPage.getContent().stream()
                .map(SecurityAuditLog::getTargetOrganizationRefId)
                .filter(refId -> refId != null && !refId.isBlank())
                .collect(Collectors.toSet());

        Map<String, String> orgNameMap = new HashMap<>();
        if (!orgRefIds.isEmpty()) {
            List<Organization> orgs = organizationRepository.findAllByRefIdIn(orgRefIds);
            for (Organization org : orgs) {
                orgNameMap.put(org.getRefId(), org.getName());
            }
        }

        List<SecurityAuditLogResponse> content = resultPage.getContent().stream()
                .map(logItem -> SecurityAuditLogResponse.builder()
                        .refId(logItem.getRefId())
                        .actorEmail(logItem.getActorEmail())
                        .action(logItem.getAction())
                        .targetOrganizationRefId(logItem.getTargetOrganizationRefId())
                        .targetOrganizationName(logItem.getTargetOrganizationRefId() != null
                                ? orgNameMap.get(logItem.getTargetOrganizationRefId())
                                : null)
                        .targetReportRefId(logItem.getTargetReportRefId())
                        .justification(logItem.getJustification())
                        .success(logItem.isSuccess())
                        .failureReason(logItem.getFailureReason())
                        .ipAddress(logItem.getIpAddress())
                        .createdAt(logItem.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<SecurityAuditLogResponse>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();
    }

    private String mapSafeSortColumn(String sort) {
        if (sort == null || sort.isBlank()) {
            return "createdAt";
        }
        return switch (sort.trim()) {
            case "action" -> "action";
            case "actorEmail" -> "actorEmail";
            case "targetOrganizationRefId" -> "targetOrganizationRefId";
            case "success" -> "success";
            default -> "createdAt";
        };
    }
}

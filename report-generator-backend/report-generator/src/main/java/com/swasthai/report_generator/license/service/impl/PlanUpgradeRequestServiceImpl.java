package com.swasthai.report_generator.license.service.impl;

import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.exception.ConflictException;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.dto.request.CreateUpgradeRequest;
import com.swasthai.report_generator.license.dto.request.UpdateUpgradeRequestStatusRequest;
import com.swasthai.report_generator.license.dto.response.AvailablePlanResponse;
import com.swasthai.report_generator.license.dto.response.LicenseResponse;
import com.swasthai.report_generator.license.dto.response.OrganizationLicenseOverviewResponse;
import com.swasthai.report_generator.license.dto.response.PlanUpgradeRequestResponse;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.entity.PlanUpgradeRequest;
import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.license.repository.PlanUpgradeRequestRepository;
import com.swasthai.report_generator.license.service.PlanUpgradeRequestService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanUpgradeRequestServiceImpl implements PlanUpgradeRequestService {

    private final PlanUpgradeRequestRepository planUpgradeRequestRepository;
    private final LicenseRepository licenseRepository;
    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ReportRepository reportRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    // ============================================================
    // ORGANIZATION ADMIN: LICENSE OVERVIEW & USAGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationLicenseOverviewResponse getOrganizationLicenseOverview() {
        Organization organization = requireOrgAdminOrganization();

        License license = licenseRepository
                .findWithPlanByOrganizationId(organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("License not found for organization."));

        Instant now = Instant.now();
        long daysRemaining = 0L;
        if (license.getExpiresAt() != null && now.isBefore(license.getExpiresAt())) {
            daysRemaining = ChronoUnit.DAYS.between(now, license.getExpiresAt());
        }

        String expiryStatus;
        if (license.getStatus() == LicenseStatus.DEACTIVATED) {
            expiryStatus = "DEACTIVATED";
        } else if (license.getStatus() == LicenseStatus.EXPIRED || (license.getExpiresAt() != null && !now.isBefore(license.getExpiresAt()))) {
            expiryStatus = "EXPIRED";
        } else if (daysRemaining <= 14) {
            expiryStatus = "EXPIRING_SOON";
        } else {
            expiryStatus = "ACTIVE";
        }

        // Staff usage
        int maxLabStaff = license.getPlan() != null && license.getPlan().getMaxLabStaff() != null
                ? license.getPlan().getMaxLabStaff()
                : 0;

        long activeStaff = userRepository.countByOrganization_IdAndRoleAndStatus(
                organization.getId(),
                Role.LAB_STAFF,
                UserStatus.ACTIVE
        );

        long totalStaff = userRepository.countByOrganization_IdAndRole(
                organization.getId(),
                Role.LAB_STAFF
        );

        int remainingSlots = (int) Math.max(0, maxLabStaff - activeStaff);
        boolean limitReached = activeStaff >= maxLabStaff;
        boolean overLimit = activeStaff > maxLabStaff;

        // Patient usage
        long totalPatients = patientRepository.countByOrganization_IdAndDeletedAtIsNull(organization.getId());

        // Report usage
        long totalReports = reportRepository.countByOrganization_IdAndDeletedAtIsNull(organization.getId());
        long finalizedReports = reportRepository.countByOrganization_IdAndStatusAndDeletedAtIsNull(
                organization.getId(),
                ReportStatus.FINALIZED
        );

        Instant startOfMonth = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        long monthlyReportsCreated = reportRepository.countByOrganization_IdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(
                organization.getId(),
                startOfMonth
        );

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        long dailyReportsCreated = reportRepository.countByOrganization_IdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(
                organization.getId(),
                startOfDay
        );

        int maxReportsPerMonth = license.getPlan() != null && license.getPlan().getMaxReportsPerMonth() != null
                ? license.getPlan().getMaxReportsPerMonth()
                : 0;

        int maxReportsPerDay = license.getPlan() != null && license.getPlan().getMaxReportsPerDay() != null
                ? license.getPlan().getMaxReportsPerDay()
                : 0;

        int remainingMonthlyReports = maxReportsPerMonth > 0
                ? (int) Math.max(0, maxReportsPerMonth - monthlyReportsCreated)
                : -1;
        boolean monthlyLimitReached = maxReportsPerMonth > 0 && monthlyReportsCreated >= maxReportsPerMonth;
        boolean dailyLimitReached = maxReportsPerDay > 0 && dailyReportsCreated >= maxReportsPerDay;

        boolean usable = license.getStatus() == LicenseStatus.ACTIVE
                && license.getExpiresAt() != null
                && now.isBefore(license.getExpiresAt());

        LicenseResponse licenseResponse = new LicenseResponse(
                license.getRefId(),
                license.getOrganization().getRefId(),
                license.getPlan().getRefId(),
                license.getPlan().getCode(),
                license.getPlan().getName(),
                license.getPlan().getMaxLabStaff(),
                maxReportsPerMonth,
                maxReportsPerDay,
                license.getStatus(),
                license.getStartedAt(),
                license.getExpiresAt(),
                usable,
                license.getCreatedAt(),
                license.getUpdatedAt()
        );

        return OrganizationLicenseOverviewResponse.builder()
                .license(licenseResponse)
                .staffUsage(OrganizationLicenseOverviewResponse.StaffUsageSummary.builder()
                        .activeStaff(activeStaff)
                        .maxLabStaff(maxLabStaff)
                        .remainingSlots(remainingSlots)
                        .totalStaff(totalStaff)
                        .limitReached(limitReached)
                        .overLimit(overLimit)
                        .build())
                .patientUsage(OrganizationLicenseOverviewResponse.PatientUsageSummary.builder()
                        .totalPatients(totalPatients)
                        .build())
                .reportUsage(OrganizationLicenseOverviewResponse.ReportUsageSummary.builder()
                        .totalReports(totalReports)
                        .finalizedReports(finalizedReports)
                        .monthlyReportsCreated(monthlyReportsCreated)
                        .maxReportsPerMonth(maxReportsPerMonth)
                        .dailyReportsCreated(dailyReportsCreated)
                        .maxReportsPerDay(maxReportsPerDay)
                        .remainingMonthlyReports(remainingMonthlyReports)
                        .monthlyLimitReached(monthlyLimitReached)
                        .dailyLimitReached(dailyLimitReached)
                        .build())
                .daysRemaining(daysRemaining)
                .expiryStatus(expiryStatus)
                .build();
    }

    // ============================================================
    // ORGANIZATION ADMIN: AVAILABLE PLANS FOR UPGRADE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AvailablePlanResponse> getAvailablePlansForUpgrade() {
        Organization organization = requireOrgAdminOrganization();

        License license = licenseRepository
                .findWithPlanByOrganizationId(organization.getId())
                .orElse(null);

        UUID currentPlanId = license != null && license.getPlan() != null ? license.getPlan().getId() : null;
        int currentMaxStaff = license != null && license.getPlan() != null && license.getPlan().getMaxLabStaff() != null
                ? license.getPlan().getMaxLabStaff()
                : 0;

        return planRepository.findAllByOrderByAnnualPriceAsc()
                .stream()
                .filter(Plan::isActive)
                .map(plan -> {
                    boolean isCurrent = currentPlanId != null && plan.getId().equals(currentPlanId);
                    boolean isUpgrade = plan.getMaxLabStaff() != null && plan.getMaxLabStaff() > currentMaxStaff;
                    return AvailablePlanResponse.builder()
                            .refId(plan.getRefId())
                            .code(plan.getCode())
                            .name(plan.getName())
                            .description(plan.getDescription())
                            .annualPrice(plan.getAnnualPrice())
                            .currency(plan.getCurrency())
                            .maxLabStaff(plan.getMaxLabStaff() != null ? plan.getMaxLabStaff() : 3)
                            .maxReportsPerMonth(plan.getMaxReportsPerMonth())
                            .maxReportsPerDay(plan.getMaxReportsPerDay())
                            .currentPlan(isCurrent)
                            .upgrade(isUpgrade)
                            .build();
                })
                .toList();
    }

    // ============================================================
    // ORGANIZATION ADMIN: CREATE UPGRADE REQUEST
    // ============================================================

    @Override
    public PlanUpgradeRequestResponse createUpgradeRequest(CreateUpgradeRequest request) {
        User currentUser = requireOrgAdminUser();
        Organization organization = currentUser.getOrganization();

        if (request == null || request.requestedPlanRefId() == null || request.requestedPlanRefId().isBlank()) {
            throw new BadRequestException("Requested plan is required.");
        }

        Plan requestedPlan = planRepository
                .findByRefId(request.requestedPlanRefId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Requested plan not found."));

        if (!requestedPlan.isActive()) {
            throw new BadRequestException("The requested plan is not currently available for subscription.");
        }

        License license = licenseRepository
                .findWithPlanByOrganizationId(organization.getId())
                .orElseThrow(() -> new ForbiddenException("An active license is required to request an upgrade."));

        Plan currentPlan = license.getPlan();
        if (currentPlan != null && requestedPlan.getId().equals(currentPlan.getId())) {
            throw new BadRequestException("Your organization is already on the " + currentPlan.getName() + " plan.");
        }

        // Check for duplicate pending requests for this plan
        boolean hasPendingDuplicate = planUpgradeRequestRepository.existsByOrganization_IdAndRequestedPlan_IdAndStatus(
                organization.getId(),
                requestedPlan.getId(),
                UpgradeRequestStatus.PENDING
        );

        if (hasPendingDuplicate) {
            throw new ConflictException("Your organization already has a pending upgrade request for the "
                    + requestedPlan.getName() + " plan. Our team will contact you shortly.");
        }

        long activeStaff = userRepository.countByOrganization_IdAndRoleAndStatus(
                organization.getId(),
                Role.LAB_STAFF,
                UserStatus.ACTIVE
        );

        PlanUpgradeRequest upgradeRequest = PlanUpgradeRequest.builder()
                .organization(organization)
                .requestedBy(currentUser)
                .requestedByEmail(currentUser.getEmail())
                .currentPlan(currentPlan)
                .currentPlanName(currentPlan != null ? currentPlan.getName() : "Unknown")
                .requestedPlan(requestedPlan)
                .requestedPlanName(requestedPlan.getName())
                .currentActiveStaffCount((int) activeStaff)
                .requestedStaffCapacity(requestedPlan.getMaxLabStaff() != null ? requestedPlan.getMaxLabStaff() : 3)
                .reason(normalizeOptional(request.reason()))
                .contactName(request.contactName().trim())
                .contactEmail(request.contactEmail().trim().toLowerCase(Locale.ROOT))
                .contactPhone(normalizeOptional(request.contactPhone()))
                .additionalMessage(normalizeOptional(request.additionalMessage()))
                .status(UpgradeRequestStatus.PENDING)
                .build();

        PlanUpgradeRequest saved = planUpgradeRequestRepository.save(upgradeRequest);

        auditLogService.recordUpgradeRequestAction(
                currentUser,
                organization,
                saved.getRefId(),
                "LICENSE_UPGRADE_REQUESTED",
                "Requested upgrade from " + (currentPlan != null ? currentPlan.getName() : "N/A") + " to " + requestedPlan.getName(),
                true,
                null,
                resolveClientIp()
        );

        return mapToResponse(saved);
    }

    // ============================================================
    // ORGANIZATION ADMIN: VIEW REQUESTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PlanUpgradeRequestResponse> getMyUpgradeRequests(int page, int size, UpgradeRequestStatus status) {
        Organization organization = requireOrgAdminOrganization();
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PlanUpgradeRequest> requestsPage;
        if (status != null) {
            requestsPage = planUpgradeRequestRepository.findAllByOrganization_IdAndStatus(
                    organization.getId(),
                    status,
                    pageable
            );
        } else {
            requestsPage = planUpgradeRequestRepository.findAllByOrganization_Id(
                    organization.getId(),
                    pageable
            );
        }

        return requestsPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanUpgradeRequestResponse getMyUpgradeRequestDetails(String refId) {
        Organization organization = requireOrgAdminOrganization();

        if (refId == null || refId.isBlank()) {
            throw new ResourceNotFoundException("Upgrade request not found.");
        }

        PlanUpgradeRequest request = planUpgradeRequestRepository
                .findByRefIdAndOrganization_Id(refId.trim(), organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found."));

        return mapToResponse(request);
    }

    @Override
    public PlanUpgradeRequestResponse cancelMyUpgradeRequest(String refId) {
        User currentUser = requireOrgAdminUser();
        Organization organization = currentUser.getOrganization();

        if (refId == null || refId.isBlank()) {
            throw new ResourceNotFoundException("Upgrade request not found.");
        }

        PlanUpgradeRequest request = planUpgradeRequestRepository
                .findByRefIdAndOrganization_Id(refId.trim(), organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found."));

        if (request.getStatus() != UpgradeRequestStatus.PENDING) {
            throw new BadRequestException("Only pending upgrade requests can be cancelled.");
        }

        request.setStatus(UpgradeRequestStatus.CANCELLED);
        PlanUpgradeRequest saved = planUpgradeRequestRepository.save(request);

        auditLogService.recordUpgradeRequestAction(
                currentUser,
                organization,
                saved.getRefId(),
                "LICENSE_UPGRADE_REQUEST_CANCELLED",
                "Cancelled upgrade request: " + saved.getRefId(),
                true,
                null,
                resolveClientIp()
        );

        return mapToResponse(saved);
    }

    // ============================================================
    // SUPER ADMIN: MANAGE UPGRADE REQUESTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PlanUpgradeRequestResponse> getAllUpgradeRequests(
            int page,
            int size,
            UpgradeRequestStatus status,
            String organizationRefId
    ) {
        requireSuperAdmin();
        validatePagination(page, size);

        UUID orgId = null;
        if (organizationRefId != null && !organizationRefId.isBlank()) {
            Organization org = organizationRepository.findByRefId(organizationRefId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
            orgId = org.getId();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planUpgradeRequestRepository.findAllWithFilters(status, orgId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanUpgradeRequestResponse getUpgradeRequestDetails(String refId) {
        requireSuperAdmin();

        if (refId == null || refId.isBlank()) {
            throw new ResourceNotFoundException("Upgrade request not found.");
        }

        PlanUpgradeRequest request = planUpgradeRequestRepository
                .findByRefId(refId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found."));

        return mapToResponse(request);
    }

    @Override
    public PlanUpgradeRequestResponse updateUpgradeRequestStatus(
            String refId,
            UpdateUpgradeRequestStatusRequest requestDto
    ) {
        User superAdmin = requireSuperAdmin();

        if (refId == null || refId.isBlank()) {
            throw new ResourceNotFoundException("Upgrade request not found.");
        }

        if (requestDto == null || requestDto.status() == null) {
            throw new BadRequestException("Status is required.");
        }

        PlanUpgradeRequest request = planUpgradeRequestRepository
                .findByRefId(refId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found."));

        UpgradeRequestStatus currentStatus = request.getStatus();
        UpgradeRequestStatus newStatus = requestDto.status();

        if (currentStatus == newStatus) {
            throw new IllegalStateException("Upgrade request is already in " + currentStatus + " status.");
        }

        if (currentStatus == UpgradeRequestStatus.APPROVED
                || currentStatus == UpgradeRequestStatus.REJECTED
                || currentStatus == UpgradeRequestStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change status of a request that is already " + currentStatus + ".");
        }

        // Validate allowed transitions:
        // PENDING -> CONTACTED or REJECTED
        // CONTACTED -> APPROVED or REJECTED
        boolean allowedTransition = switch (currentStatus) {
            case PENDING -> newStatus == UpgradeRequestStatus.CONTACTED || newStatus == UpgradeRequestStatus.REJECTED;
            case CONTACTED -> newStatus == UpgradeRequestStatus.APPROVED || newStatus == UpgradeRequestStatus.REJECTED;
            default -> false;
        };

        if (!allowedTransition) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus
                    + ". Allowed transitions: PENDING -> CONTACTED/REJECTED, CONTACTED -> APPROVED/REJECTED.");
        }

        if (newStatus == UpgradeRequestStatus.APPROVED) {
            // Apply upgraded plan to the organization's license atomically
            License license = licenseRepository
                    .findByOrganizationIdForUpdate(request.getOrganization().getId())
                    .orElse(null);
            if (license != null) {
                license.setPlan(request.getRequestedPlan());
                licenseRepository.save(license);
            }
        }

        if (newStatus == UpgradeRequestStatus.REJECTED) {
            if (requestDto.rejectionReason() == null || requestDto.rejectionReason().isBlank()) {
                throw new BadRequestException("A rejection reason is required when rejecting an upgrade request.");
            }
            request.setRejectionReason(requestDto.rejectionReason().trim());
        }

        if (requestDto.adminNotes() != null) {
            request.setAdminNotes(normalizeOptional(requestDto.adminNotes()));
        }

        request.setStatus(newStatus);
        request.setReviewedBy(superAdmin);
        request.setReviewedAt(Instant.now());

        PlanUpgradeRequest saved = planUpgradeRequestRepository.save(request);

        String auditAction = switch (newStatus) {
            case CONTACTED -> "LICENSE_UPGRADE_REQUEST_CONTACTED";
            case APPROVED -> "LICENSE_UPGRADE_REQUEST_APPROVED";
            case REJECTED -> "LICENSE_UPGRADE_REQUEST_REJECTED";
            default -> "LICENSE_UPGRADE_REQUEST_UPDATED";
        };

        auditLogService.recordUpgradeRequestAction(
                superAdmin,
                saved.getOrganization(),
                saved.getRefId(),
                auditAction,
                "Status updated to " + newStatus + (newStatus == UpgradeRequestStatus.REJECTED ? ": " + saved.getRejectionReason() : ""),
                true,
                null,
                resolveClientIp()
        );

        return mapToResponse(saved);
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private Organization requireOrgAdminOrganization() {
        return requireOrgAdminUser().getOrganization();
    }

    private User requireOrgAdminUser() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ORG_ADMIN) {
            throw new ForbiddenException("Only ORG_ADMIN can access organization license details.");
        }

        Organization organization = currentUser.getOrganization();
        if (organization == null) {
            throw new ForbiddenException("User does not belong to any organization.");
        }

        return currentUser;
    }

    private User requireSuperAdmin() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can manage upgrade requests.");
        }
        return currentUser;
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be negative.");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100.");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveClientIp() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest req = servletAttrs.getRequest();
                String xfHeader = req.getHeader("X-Forwarded-For");
                if (xfHeader != null && !xfHeader.isBlank()) {
                    return xfHeader.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private PlanUpgradeRequestResponse mapToResponse(PlanUpgradeRequest r) {
        return PlanUpgradeRequestResponse.builder()
                .refId(r.getRefId())
                .organizationRefId(r.getOrganization() != null ? r.getOrganization().getRefId() : null)
                .organizationName(r.getOrganization() != null ? r.getOrganization().getName() : null)
                .requestedByEmail(r.getRequestedByEmail())
                .currentPlanRefId(r.getCurrentPlan() != null ? r.getCurrentPlan().getRefId() : null)
                .currentPlanName(r.getCurrentPlanName())
                .requestedPlanRefId(r.getRequestedPlan() != null ? r.getRequestedPlan().getRefId() : null)
                .requestedPlanName(r.getRequestedPlanName())
                .currentActiveStaffCount(r.getCurrentActiveStaffCount())
                .requestedStaffCapacity(r.getRequestedStaffCapacity())
                .reason(r.getReason())
                .contactName(r.getContactName())
                .contactEmail(r.getContactEmail())
                .contactPhone(r.getContactPhone())
                .additionalMessage(r.getAdditionalMessage())
                .status(r.getStatus())
                .reviewedByEmail(r.getReviewedBy() != null ? r.getReviewedBy().getEmail() : null)
                .reviewedAt(r.getReviewedAt())
                .adminNotes(r.getAdminNotes())
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}

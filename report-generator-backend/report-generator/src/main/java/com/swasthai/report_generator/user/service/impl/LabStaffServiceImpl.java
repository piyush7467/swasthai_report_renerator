package com.swasthai.report_generator.user.service.impl;

import com.swasthai.report_generator.auth.repository.RefreshTokenRepository;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.exception.ConflictException;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.user.dto.request.CreateLabStaffRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.LabStaffDetailsResponse;
import com.swasthai.report_generator.user.dto.response.LabStaffSummaryResponse;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.LabStaffService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class LabStaffServiceImpl implements LabStaffService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int CLEANUP_RETENTION_DAYS = 10;

    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;
    private final ReportRepository reportRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public LabStaffSummaryResponse getLabStaffSummary() {
        Organization organization = requireOrgAdminOrganization();

        License license = licenseRepository
                .findWithPlanByOrganizationId(organization.getId())
                .orElse(null);

        boolean usable = license != null
                && license.getStatus() == LicenseStatus.ACTIVE
                && license.getExpiresAt() != null
                && Instant.now().isBefore(license.getExpiresAt());

        int maxLabStaff = (usable && license.getPlan() != null && license.getPlan().getMaxLabStaff() != null)
                ? license.getPlan().getMaxLabStaff()
                : 0;

        long totalStaff = userRepository.countByOrganization_IdAndRole(organization.getId(), Role.LAB_STAFF);
        long activeStaff = userRepository.countByOrganization_IdAndRoleAndStatus(organization.getId(), Role.LAB_STAFF, UserStatus.ACTIVE);

        int remainingSlots = Math.max(0, maxLabStaff - (int) activeStaff);
        boolean limitReached = activeStaff >= maxLabStaff;

        return LabStaffSummaryResponse.builder()
                .totalStaff(totalStaff)
                .activeStaff(activeStaff)
                .maxLabStaff(maxLabStaff)
                .remainingSlots(remainingSlots)
                .limitReached(limitReached)
                .planCode(license != null && license.getPlan() != null ? license.getPlan().getCode() : null)
                .planName(license != null && license.getPlan() != null ? license.getPlan().getName() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse getLabStaffList(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            UserStatus status,
            String search
    ) {
        Organization organization = requireOrgAdminOrganization();
        validatePagination(page, size);

        Sort.Direction direction = resolveSortDirection(sortDirection);
        String sortField = resolveSortField(sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<User> usersPage;
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        if (trimmedSearch != null) {
            if (status != null) {
                usersPage = userRepository.findLabStaffByOrganizationAndStatusAndSearch(
                        organization.getId(),
                        Role.LAB_STAFF,
                        status,
                        trimmedSearch,
                        pageable
                );
            } else {
                usersPage = userRepository.findLabStaffByOrganizationAndSearch(
                        organization.getId(),
                        Role.LAB_STAFF,
                        trimmedSearch,
                        pageable
                );
            }
        } else {
            if (status != null) {
                usersPage = userRepository.findAllByOrganization_IdAndRoleAndStatus(
                        organization.getId(),
                        Role.LAB_STAFF,
                        status,
                        pageable
                );
            } else {
                usersPage = userRepository.findAllByOrganization_IdAndRole(
                        organization.getId(),
                        Role.LAB_STAFF,
                        pageable
                );
            }
        }

        List<UserResponse> content = usersPage.getContent()
                .stream()
                .map(this::mapToUserResponse)
                .toList();

        return UserPageResponse.builder()
                .content(content)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LabStaffDetailsResponse getLabStaffDetails(String refId) {
        Organization organization = requireOrgAdminOrganization();
        User staff = findLabStaffInOrganization(refId, organization);

        long reportsCreated = reportRepository.countByCreatedBy_IdAndDeletedAtIsNull(staff.getId());
        long reportsFinalized = reportRepository.countByFinalizedBy_IdAndDeletedAtIsNull(staff.getId());

        Instant inactiveAt = staff.getInactiveAt();
        Instant eligibleForCleanupAt = inactiveAt != null ? inactiveAt.plus(CLEANUP_RETENTION_DAYS, ChronoUnit.DAYS) : null;
        Boolean cleanupEligible = eligibleForCleanupAt != null && Instant.now().isAfter(eligibleForCleanupAt);
        String deactivatedByName = staff.getDeactivatedBy() != null ? staff.getDeactivatedBy().getName() : null;

        return LabStaffDetailsResponse.builder()
                .refId(staff.getRefId())
                .name(staff.getName())
                .email(staff.getEmail())
                .role(staff.getRole())
                .status(staff.getStatus())
                .organizationRefId(organization.getRefId())
                .organizationName(organization.getName())
                .lastLoginAt(staff.getLastLoginAt())
                .inactiveAt(inactiveAt)
                .eligibleForCleanupAt(eligibleForCleanupAt)
                .cleanupEligible(cleanupEligible)
                .deactivatedByName(deactivatedByName)
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .reportsCreated(reportsCreated)
                .reportsFinalized(reportsFinalized)
                .build();
    }

    @Override
    public UserResponse createLabStaff(CreateLabStaffRequest request) {
        User currentUser = requireOrgAdminUser();
        Organization organization = currentUser.getOrganization();

        if (request == null) {
            throw new BadRequestException("Staff creation request is required.");
        }

        String name = normalizeName(request.getName());
        String email = normalizeEmail(request.getEmail());

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceAlreadyExistsException("Email is already registered.");
        }

        // Concurrency-safe license check: Lock license row for this organization
        License license = licenseRepository
                .findByOrganizationIdForUpdate(organization.getId())
                .orElseThrow(() -> new ForbiddenException("An active license is required to add lab staff."));

        if (!isCurrentlyUsable(license)) {
            throw new ForbiddenException("Your organization license has expired or is inactive. Please renew your subscription to add staff.");
        }

        int maxLabStaff = license.getPlan() != null && license.getPlan().getMaxLabStaff() != null
                ? license.getPlan().getMaxLabStaff()
                : 0;

        long currentActiveStaff = userRepository.countByOrganization_IdAndRoleAndStatus(
                organization.getId(),
                Role.LAB_STAFF,
                UserStatus.ACTIVE
        );

        if (currentActiveStaff >= maxLabStaff) {
            throw new ConflictException("Lab staff limit reached for this organization (" + maxLabStaff + " staff max). Please upgrade your subscription plan to add more staff.");
        }

        User staff = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(organization)
                .build();

        User saved = userRepository.save(staff);

        auditLogService.recordStaffAction(
                currentUser,
                organization,
                saved,
                "LAB_STAFF_CREATED",
                "Created active lab staff account: " + saved.getEmail(),
                true,
                null,
                resolveClientIp()
        );

        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse deactivateLabStaff(String refId) {
        User currentUser = requireOrgAdminUser();
        Organization organization = currentUser.getOrganization();
        User staff = findLabStaffInOrganization(refId, organization);

        if (staff.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Staff member is already inactive.");
        }

        Instant now = Instant.now();
        staff.setStatus(UserStatus.INACTIVE);
        staff.setInactiveAt(now);
        staff.setDeactivatedBy(currentUser);

        // Immediately revoke all active refresh tokens for this staff member
        refreshTokenRepository.revokeAllActiveByUserId(staff.getId(), now);

        User saved = userRepository.save(staff);

        auditLogService.recordStaffAction(
                currentUser,
                organization,
                saved,
                "LAB_STAFF_DEACTIVATED",
                "Deactivated lab staff account: " + saved.getEmail(),
                true,
                null,
                resolveClientIp()
        );

        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse updateLabStaffStatus(String refId, UpdateUserStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Status is required.");
        }

        UserStatus newStatus = request.getStatus();
        if (newStatus == UserStatus.INACTIVE) {
            return deactivateLabStaff(refId);
        }

        User currentUser = requireOrgAdminUser();
        Organization organization = currentUser.getOrganization();
        User staff = findLabStaffInOrganization(refId, organization);

        if (staff.getStatus() == newStatus) {
            throw new IllegalStateException("Staff member is already in " + newStatus + " status.");
        }

        // If activating, enforce the license limit with row-locking
        License license = licenseRepository
                .findByOrganizationIdForUpdate(organization.getId())
                .orElseThrow(() -> new ForbiddenException("An active license is required to activate lab staff."));

        if (!isCurrentlyUsable(license)) {
            throw new ForbiddenException("Your organization license has expired or is inactive. Please renew your subscription to activate staff.");
        }

        int maxLabStaff = license.getPlan() != null && license.getPlan().getMaxLabStaff() != null
                ? license.getPlan().getMaxLabStaff()
                : 0;

        long currentActiveStaff = userRepository.countByOrganization_IdAndRoleAndStatus(
                organization.getId(),
                Role.LAB_STAFF,
                UserStatus.ACTIVE
        );

        if (currentActiveStaff >= maxLabStaff) {
            throw new ConflictException("Cannot activate staff member. Lab staff limit reached (" + maxLabStaff + " active staff max). Please upgrade your subscription plan or deactivate another staff member first.");
        }

        staff.setStatus(UserStatus.ACTIVE);
        staff.setInactiveAt(null);
        staff.setDeactivatedBy(null);
        User saved = userRepository.save(staff);

        auditLogService.recordStaffAction(
                currentUser,
                organization,
                saved,
                "LAB_STAFF_REACTIVATED",
                "Reactivated lab staff account: " + saved.getEmail(),
                true,
                null,
                resolveClientIp()
        );

        return mapToUserResponse(saved);
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

    @Override
    public void deleteLabStaff(String refId) {
        Organization organization = requireOrgAdminOrganization();
        User staff = findLabStaffInOrganization(refId, organization);

        long reportsCreated = reportRepository.countByCreatedBy_IdAndDeletedAtIsNull(staff.getId());
        long reportsFinalized = reportRepository.countByFinalizedBy_IdAndDeletedAtIsNull(staff.getId());

        if (reportsCreated > 0 || reportsFinalized > 0) {
            throw new ConflictException("Staff member cannot be permanently deleted because they have created or finalized diagnostic reports. Under laboratory data integrity rules, please deactivate this staff member instead.");
        }

        refreshTokenRepository.deleteByUser_Id(staff.getId());
        userRepository.delete(staff);
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
            throw new ForbiddenException("Only ORG_ADMIN can manage lab staff.");
        }

        Organization organization = currentUser.getOrganization();
        if (organization == null) {
            throw new ForbiddenException("User does not belong to any organization.");
        }

        return currentUser;
    }

    private User findLabStaffInOrganization(String refId, Organization organization) {
        if (refId == null || refId.isBlank()) {
            throw new ResourceNotFoundException("Staff member not found.");
        }

        User staff = userRepository.findByRefIdAndOrganization_Id(refId.trim(), organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));

        if (staff.getRole() != Role.LAB_STAFF) {
            throw new BadRequestException("Target user is not a lab staff member.");
        }

        return staff;
    }

    private boolean isCurrentlyUsable(License license) {
        return license != null
                && license.getStatus() == LicenseStatus.ACTIVE
                && license.getExpiresAt() != null
                && Instant.now().isBefore(license.getExpiresAt());
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 150) {
            throw new BadRequestException("Name must be between 2 and 150 characters.");
        }
        return trimmed;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("A valid email address is required.");
        }
        return trimmed;
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be negative.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
    }

    private Sort.Direction resolveSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.DESC;
        }
        try {
            return Sort.Direction.fromString(sortDirection.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sort direction. Use ASC or DESC.");
        }
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim()) {
            case "name" -> "name";
            case "email" -> "email";
            case "status" -> "status";
            case "lastLoginAt" -> "lastLoginAt";
            case "inactiveAt" -> "inactiveAt";
            case "createdAt" -> "createdAt";
            default -> throw new BadRequestException("Unsupported sort field: " + sortBy);
        };
    }

    private UserResponse mapToUserResponse(User user) {
        Instant inactiveAt = user.getInactiveAt();
        Instant eligibleForCleanupAt = inactiveAt != null ? inactiveAt.plus(CLEANUP_RETENTION_DAYS, ChronoUnit.DAYS) : null;
        Boolean cleanupEligible = eligibleForCleanupAt != null && Instant.now().isAfter(eligibleForCleanupAt);
        String deactivatedByName = user.getDeactivatedBy() != null ? user.getDeactivatedBy().getName() : null;

        long reportsCreated = reportRepository.countByCreatedBy_IdAndDeletedAtIsNull(user.getId());
        long reportsFinalized = reportRepository.countByFinalizedBy_IdAndDeletedAtIsNull(user.getId());

        return UserResponse.builder()
                .refId(user.getRefId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .organizationRefId(user.getOrganization() != null ? user.getOrganization().getRefId() : null)
                .lastLoginAt(user.getLastLoginAt())
                .inactiveAt(inactiveAt)
                .eligibleForCleanupAt(eligibleForCleanupAt)
                .cleanupEligible(cleanupEligible)
                .deactivatedByName(deactivatedByName)
                .reportsCreated(reportsCreated)
                .reportsFinalized(reportsFinalized)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

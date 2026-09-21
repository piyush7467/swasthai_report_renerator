package com.swasthai.report_generator.license.service.impl;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.config.LicenseProperties;
import com.swasthai.report_generator.license.dto.request.ActivateLicenseRequest;
import com.swasthai.report_generator.license.dto.request.RenewLicenseRequest;
import com.swasthai.report_generator.license.dto.response.LicenseResponse;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.license.service.LicenseService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LicenseServiceImpl
        implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CurrentOrganizationService currentOrganizationService;
    private final LicenseProperties licenseProperties;

    // SUPER ADMIN - ACTIVATE

    @Override
    public LicenseResponse activateLicense(
            String organizationRefId,
            ActivateLicenseRequest request) {

        requireSuperAdmin();

        if (request == null) {
            throw new IllegalArgumentException(
                    "License activation request is required.");
        }

        Organization organization = findOrganization(organizationRefId);

        validateOrganizationCanBeLicensed(
                organization);

        Plan plan = findActivePlan(request.planRefId());

        /*
         * Lock the existing license if present.
         *
         * If no license exists, the database UNIQUE constraint
         * on organization_id remains the final race-condition
         * protection.
         */
        var existing = licenseRepository.findByOrganizationIdForUpdate(
                organization.getId());

        if (existing.isPresent()) {

            License existingLicense = existing.get();

            refreshStatusIfExpired(
                    existingLicense);

            if (isCurrentlyUsable(existingLicense)) {

                throw new ResourceAlreadyExistsException(
                        "Organization already has an active license.");
            }

            /*
             * An expired license can be reactivated.
             *
             * We reuse the single license row rather than
             * creating another row.
             */
            activateExistingLicense(
                    existingLicense,
                    plan,
                    request.paymentReference());

            return mapToResponse(
                    existingLicense);
        }

        Instant startedAt = Instant.now();

        Instant expiresAt = startedAt.plus(
                licenseProperties.getDurationDays(),
                ChronoUnit.DAYS);

        User verifier = currentUserService.getCurrentUser();

        License license = License.builder()
                .organization(organization)
                .plan(plan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .paymentReference(
                        normalizePaymentReference(
                                request.paymentReference()))
                .paymentVerifiedBy(verifier)
                .paymentVerifiedAt(startedAt)
                .lockVersion(0L)
                .build();

        try {

            License saved = licenseRepository.saveAndFlush(
                    license);

            return mapToResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            /*
             * Another concurrent SUPER_ADMIN operation may have
             * inserted the license after our initial lookup.
             *
             * Never silently create a second license.
             */
            throw new ResourceAlreadyExistsException(
                    "Organization license already exists. Reload and try again.");
        }
    }

    // SUPER ADMIN - RENEW

    @Override
    public LicenseResponse renewLicense(
            String organizationRefId,
            RenewLicenseRequest request) {

        requireSuperAdmin();

        if (request == null) {
            throw new IllegalArgumentException(
                    "License renewal request is required.");
        }

        Organization organization = findOrganization(organizationRefId);

        validateOrganizationCanBeLicensed(
                organization);

        Plan plan = findActivePlan(request.planRefId());

        License license = licenseRepository
                .findByOrganizationIdForUpdate(
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found for organization."));

        Instant now = Instant.now();

        /*
         * If the license is still active, preserve the existing
         * paid period and add configured duration to its expiry.
         *
         * If expired, start a completely new configured period now.
         */
        Instant newStartedAt;
        Instant newExpiresAt;

        if (license.getStatus() == LicenseStatus.DEACTIVATED) {

            long remainingSeconds = license.getPausedRemainingSeconds() != null
                    ? license.getPausedRemainingSeconds()
                    : 0L;

            newStartedAt = now;

            newExpiresAt = now.plusSeconds(remainingSeconds)
                    .plus(licenseProperties.getDurationDays(), ChronoUnit.DAYS);

            license.setPausedRemainingSeconds(null);
            license.setDeactivatedAt(null);

        } else if (isCurrentlyUsable(license)) {

            newStartedAt = license.getStartedAt();

            newExpiresAt = license.getExpiresAt()
                    .plus(licenseProperties.getDurationDays(), ChronoUnit.DAYS);

        } else {

            newStartedAt = now;

            newExpiresAt = now.plus(licenseProperties.getDurationDays(), ChronoUnit.DAYS);
        }

        license.setPlan(plan);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setStartedAt(newStartedAt);
        license.setExpiresAt(newExpiresAt);
        license.setPaymentReference(
                normalizePaymentReference(
                        request.paymentReference()));
        license.setPaymentVerifiedBy(
                currentUserService.getCurrentUser());
        license.setPaymentVerifiedAt(now);

        return mapToResponse(
                licenseRepository.saveAndFlush(
                        license));
    }

    // SUPER ADMIN - DEACTIVATE (FREEZE REMAINING DURATION)

    @Override
    public LicenseResponse deactivateLicense(
            String organizationRefId) {

        requireSuperAdmin();

        Organization organization = findOrganization(organizationRefId);

        License license = licenseRepository
                .findByOrganizationIdForUpdate(
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found for organization."));

        if (license.getStatus() == LicenseStatus.DEACTIVATED) {
            throw new IllegalStateException(
                    "Organization license is already deactivated.");
        }

        refreshStatusIfExpired(license);

        if (license.getStatus() == LicenseStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Cannot deactivate an expired license.");
        }

        Instant now = Instant.now();
        long remainingSeconds = 0;
        if (license.getExpiresAt() != null && now.isBefore(license.getExpiresAt())) {
            remainingSeconds = ChronoUnit.SECONDS.between(now, license.getExpiresAt());
        }

        license.setStatus(LicenseStatus.DEACTIVATED);
        license.setPausedRemainingSeconds(remainingSeconds);
        license.setDeactivatedAt(now);

        return mapToResponse(
                licenseRepository.saveAndFlush(license));
    }

    // SUPER ADMIN - REACTIVATE (RESUME REMAINING DURATION FROM TODAY)

    @Override
    public LicenseResponse reactivateLicense(
            String organizationRefId) {

        requireSuperAdmin();

        Organization organization = findOrganization(organizationRefId);
        validateOrganizationCanBeLicensed(organization);

        License license = licenseRepository
                .findByOrganizationIdForUpdate(
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found for organization."));

        if (license.getStatus() == LicenseStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Organization license is already active.");
        }

        if (license.getStatus() == LicenseStatus.EXPIRED) {
            throw new IllegalStateException(
                    "License has expired. Please renew or activate a new license.");
        }

        Instant now = Instant.now();
        long remainingSeconds = license.getPausedRemainingSeconds() != null
                ? license.getPausedRemainingSeconds()
                : 0L;

        if (remainingSeconds <= 0) {
            throw new IllegalStateException(
                    "No remaining validity left to resume. Please renew the license.");
        }

        Instant newExpiresAt = now.plusSeconds(remainingSeconds);

        license.setStatus(LicenseStatus.ACTIVE);
        license.setExpiresAt(newExpiresAt);
        license.setPausedRemainingSeconds(null);
        license.setDeactivatedAt(null);

        return mapToResponse(
                licenseRepository.saveAndFlush(license));
    }

    // ORGANIZATION - OWN LICENSE

    @Override
    @Transactional(readOnly = true)
    public LicenseResponse getOwnLicense() {

        Organization organization = currentOrganizationService
                .getCurrentOrganization();

        License license = licenseRepository
                .findWithPlanByOrganizationId(
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found."));

        return mapToResponse(license);
    }

    // SUPER ADMIN - ANY ORGANIZATION

    @Override
    @Transactional(readOnly = true)
    public LicenseResponse getLicenseForOrganization(
            String organizationRefId) {

        requireSuperAdmin();

        Organization organization = findOrganization(organizationRefId);

        License license = licenseRepository
                .findWithPlanByOrganizationId(
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "License not found."));

        return mapToResponse(license);
    }

    // MANDATORY REPORT-GENERATION GUARD

    @Override
    @Transactional(readOnly = true)
    public void requireActiveLicenseForOrganization(
            UUID organizationId) {

        if (organizationId == null) {
            throw new AccessDeniedException(
                    "Organization is required.");
        }

        License license = licenseRepository
                .findByOrganization_Id(
                        organizationId)
                .orElseThrow(() -> new AccessDeniedException(
                        "An active license is required to create reports."));

        Instant now = Instant.now();

        /*
         * License status AND expiration are checked.
         *
         * Plan.active is deliberately NOT checked here.
         *
         * Deactivating a plan prevents NEW licenses from using it,
         * but does not invalidate already-paid licenses.
         */
        if (license.getStatus() != LicenseStatus.ACTIVE
                || license.getExpiresAt() == null
                || !now.isBefore(
                        license.getExpiresAt())) {

            throw new AccessDeniedException(
                    "Your organization license has expired. "
                            + "Please renew the license to create reports.");
        }
    }
    // HELPERS

    private Organization findOrganization(
            String organizationRefId) {

        if (organizationRefId == null
                || organizationRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Organization reference ID is required.");
        }

        return organizationRepository
                .findByRefId(
                        organizationRefId.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found."));
    }

    private Plan findActivePlan(
            String planRefId) {

        if (planRefId == null
                || planRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Plan reference ID is required.");
        }

        Plan plan = planRepository
                .findByRefId(
                        planRefId.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found."));

        if (!plan.isActive()) {

            throw new IllegalArgumentException(
                    "Selected plan is inactive.");
        }

        return plan;
    }

    private void validateOrganizationCanBeLicensed(
            Organization organization) {

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Only active organizations can have an active license.");
        }
    }

    private void activateExistingLicense(
            License license,
            Plan plan,
            String paymentReference) {

        Instant now = Instant.now();

        license.setPlan(plan);
        license.setStatus(
                LicenseStatus.ACTIVE);
        license.setStartedAt(now);
        license.setExpiresAt(
                now.plus(licenseProperties.getDurationDays(), ChronoUnit.DAYS));

        license.setPaymentReference(
                normalizePaymentReference(
                        paymentReference));

        license.setPaymentVerifiedBy(
                currentUserService.getCurrentUser());

        license.setPaymentVerifiedAt(now);

        licenseRepository.saveAndFlush(
                license);
    }

    private void refreshStatusIfExpired(
            License license) {

        if (license.getStatus() == LicenseStatus.ACTIVE
                && license.getExpiresAt() != null
                && !Instant.now().isBefore(
                        license.getExpiresAt())) {

            license.setStatus(
                    LicenseStatus.EXPIRED);

            licenseRepository.saveAndFlush(
                    license);
        }
    }

    private boolean isCurrentlyUsable(
            License license) {

        return license.getStatus() == LicenseStatus.ACTIVE
                && license.getExpiresAt() != null
                && Instant.now().isBefore(
                        license.getExpiresAt());
    }

    private String normalizePaymentReference(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Payment reference is too long.");
        }

        return normalized;
    }

    private void requireSuperAdmin() {

        User user = currentUserService.getCurrentUser();

        if (user == null
                || user.getRole() != Role.SUPER_ADMIN) {

            throw new AccessDeniedException(
                    "Only SUPER_ADMIN can manage licenses.");
        }
    }

    private LicenseResponse mapToResponse(
            License license) {

        boolean usable = isCurrentlyUsable(license);

        return new LicenseResponse(
                license.getRefId(),
                license.getOrganization().getRefId(),
                license.getPlan().getRefId(),
                license.getPlan().getCode(),
                license.getPlan().getName(),
                license.getStatus(),
                license.getStartedAt(),
                license.getExpiresAt(),
                usable,
                license.getCreatedAt(),
                license.getUpdatedAt());
    }
}
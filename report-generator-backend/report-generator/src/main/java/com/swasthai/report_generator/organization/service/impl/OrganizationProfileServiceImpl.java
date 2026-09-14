package com.swasthai.report_generator.organization.service.impl;

import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationProfileRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationProfileResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationProfile;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.organization.service.OrganizationProfileService;
import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationProfileServiceImpl
        implements OrganizationProfileService {

    private final OrganizationProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    // ORG ADMIN

    @Override
    public OrganizationProfileResponse getMyProfile() {

        User currentUser =
                currentUserService.getCurrentUser();

        Organization organization =
                requireOrganizationUser(currentUser);

        return mapToResponse(
                getRequiredProfile(organization.getId())
        );
    }

    @Override
    public OrganizationProfileResponse updateMyProfile(
            UpdateOrganizationProfileRequest request
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        requireOrgAdmin(currentUser);

        Organization organization =
                requireOrganizationUser(currentUser);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        applyUpdates(profile, request);

        return mapToResponse(
                profileRepository.save(profile)
        );
    }

    @Override
    public OrganizationProfileResponse getProfileByOrganizationRefId(
            String organizationRefId
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        return mapToResponse(
                getRequiredProfile(organization.getId())
        );
    }

    @Override
    public OrganizationProfileResponse updateProfileByOrganizationRefId(
            String organizationRefId,
            UpdateOrganizationProfileRequest request
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        applyUpdates(profile, request);

        return mapToResponse(
                profileRepository.save(profile)
        );
    }

    // ORG ADMIN LOGO

    @Override
    public OrganizationProfileResponse uploadMyLogo(
            MultipartFile file
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        requireOrgAdmin(currentUser);

        Organization organization =
                requireOrganizationUser(currentUser);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        String newKey =
                fileStorageService.storeOrganizationLogo(
                        organization.getRefId(),
                        file
                );

        try {

            profile.setLogoStorageKey(newKey);

            profileRepository.save(profile);

        } catch (RuntimeException exception) {

            try {
                fileStorageService.delete(newKey);
            } catch (RuntimeException ignored) {
            }

            throw exception;
        }

        return mapToResponse(profile);
    }

    @Override
    public OrganizationProfileResponse deleteMyLogo() {

        User currentUser =
                currentUserService.getCurrentUser();

        requireOrgAdmin(currentUser);

        Organization organization =
                requireOrganizationUser(currentUser);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        profile.setLogoStorageKey(null);

        profileRepository.save(profile);

        return mapToResponse(profile);
    }

    // ORG ADMIN SIGNATURE

    @Override
    public OrganizationProfileResponse uploadMySignature(
            MultipartFile file
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        requireOrgAdmin(currentUser);

        Organization organization =
                requireOrganizationUser(currentUser);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        String newKey =
                fileStorageService.storeOrganizationSignature(
                        organization.getRefId(),
                        file
                );

        try {

            profile.setSignatureStorageKey(newKey);
            profile.setSignatureOwnerRefId(currentUser.getRefId());

            profileRepository.save(profile);

        } catch (RuntimeException exception) {

            try {
                fileStorageService.delete(newKey);
            } catch (RuntimeException ignored) {
            }

            throw exception;
        }

        return mapToResponse(profile);
    }

    @Override
    public OrganizationProfileResponse deleteMySignature() {

        User currentUser =
                currentUserService.getCurrentUser();

        requireOrgAdmin(currentUser);

        Organization organization =
                requireOrganizationUser(currentUser);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        profile.setSignatureStorageKey(null);
        profile.setSignatureOwnerRefId(null);

        profileRepository.save(profile);

        return mapToResponse(profile);
    }

    // SUPER ADMIN LOGO

    @Override
    public OrganizationProfileResponse uploadLogoForOrganization(
            String organizationRefId,
            MultipartFile file
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        String newKey =
                fileStorageService.storeOrganizationLogo(
                        organization.getRefId(),
                        file
                );

        try {

            profile.setLogoStorageKey(newKey);

            profileRepository.save(profile);

        } catch (RuntimeException exception) {

            try {
                fileStorageService.delete(newKey);
            } catch (RuntimeException ignored) {
            }

            throw exception;
        }

        return mapToResponse(profile);
    }

    @Override
    public OrganizationProfileResponse deleteLogoForOrganization(
            String organizationRefId
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        profile.setLogoStorageKey(null);

        profileRepository.save(profile);

        return mapToResponse(profile);
    }

    // SUPER ADMIN SIGNATURE

    @Override
    public OrganizationProfileResponse uploadSignatureForOrganization(
            String organizationRefId,
            MultipartFile file
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        validateActiveOrganization(organization);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        String currentOwnerRefId = profile.getSignatureOwnerRefId();
        if (currentOwnerRefId == null || currentOwnerRefId.isBlank()) {
            throw new IllegalStateException(
                    "Cannot upload signature without an authorized organization signature owner. An ORG_ADMIN must upload the initial signature."
            );
        }

        User signatureOwner = userRepository
                .findByRefIdWithOrganization(currentOwnerRefId.trim())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Configured organization signature owner was not found."
                        )
                );

        if (signatureOwner.getOrganization() == null
                || !signatureOwner.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalStateException(
                    "Configured signature owner does not belong to the organization."
            );
        }

        if (signatureOwner.getRole() != Role.ORG_ADMIN) {
            throw new IllegalStateException(
                    "Configured signature owner must be an ORG_ADMIN."
            );
        }

        if (signatureOwner.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Configured signature owner is inactive."
            );
        }

        String newKey =
                fileStorageService.storeOrganizationSignature(
                        organization.getRefId(),
                        file
                );

        try {

            profile.setSignatureStorageKey(newKey);

            profileRepository.save(profile);

        } catch (RuntimeException exception) {

            try {
                fileStorageService.delete(newKey);
            } catch (RuntimeException ignored) {
            }

            throw exception;
        }

        return mapToResponse(profile);
    }

    @Override
    public OrganizationProfileResponse deleteSignatureForOrganization(
            String organizationRefId
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganizationByRefId(organizationRefId);

        OrganizationProfile profile =
                getRequiredProfile(organization.getId());

        profile.setSignatureStorageKey(null);
        profile.setSignatureOwnerRefId(null);

        profileRepository.save(profile);

        return mapToResponse(profile);
    }

    // AUTHORIZATION

    private void requireSuperAdmin() {

        User user =
                currentUserService.getCurrentUser();

        if (user.getRole() != Role.SUPER_ADMIN) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "Only SUPER_ADMIN can perform this operation."
            );
        }
    }

    private void requireOrgAdmin(User user) {

        if (user.getRole() != Role.ORG_ADMIN) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "Only ORG_ADMIN can perform this operation."
            );
        }
    }

    private Organization requireOrganizationUser(
            User user
    ) {

        if (user.getOrganization() == null) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "User is not associated with an organization."
            );
        }

        return user.getOrganization();
    }

    // ORGANIZATION

    private Organization getOrganizationByRefId(
            String organizationRefId
    ) {

        if (organizationRefId == null
                || organizationRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Organization reference ID is required."
            );
        }

        return organizationRepository
                .findByRefId(
                        organizationRefId.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization not found."
                        )
                );
    }

    private void validateActiveOrganization(
            Organization organization
    ) {

        if (organization.getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Organization is not active."
            );
        }
    }

    // PROFILE

    private OrganizationProfile getRequiredProfile(
            UUID organizationId
    ) {

        return profileRepository
                .findByOrganization_Id(organizationId)
                .orElseGet(() -> {
                    Organization organization = organizationRepository.findById(organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
                    return profileRepository.save(
                            OrganizationProfile.builder()
                                    .organization(organization)
                                    .build()
                    );
                });
    }

    private void applyUpdates(
            OrganizationProfile profile,
            UpdateOrganizationProfileRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Profile update request is required."
            );
        }

        profile.setAddressLine1(
                normalize(request.getAddressLine1())
        );

        profile.setAddressLine2(
                normalize(request.getAddressLine2())
        );

        profile.setCity(
                normalize(request.getCity())
        );

        profile.setState(
                normalize(request.getState())
        );

        profile.setPostalCode(
                normalize(request.getPostalCode())
        );

        profile.setCountry(
                normalize(request.getCountry())
        );

        profile.setPhone(
                normalize(request.getPhone())
        );

        profile.setAlternatePhone(
                normalize(request.getAlternatePhone())
        );

        profile.setEmail(
                normalize(request.getEmail())
        );

        profile.setWebsite(
                normalize(request.getWebsite())
        );

        profile.setReportFooterText(
                normalize(request.getReportFooterText())
        );

        profile.setReportDisclaimer(
                normalize(request.getReportDisclaimer())
        );
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    // RESPONSE

    private OrganizationProfileResponse mapToResponse(
            OrganizationProfile profile
    ) {

        Organization organization =
                profile.getOrganization();

        return OrganizationProfileResponse.builder()
                .organizationRefId(
                        organization.getRefId()
                )
                .organizationName(
                        organization.getName()
                )
                .addressLine1(
                        profile.getAddressLine1()
                )
                .addressLine2(
                        profile.getAddressLine2()
                )
                .city(
                        profile.getCity()
                )
                .state(
                        profile.getState()
                )
                .postalCode(
                        profile.getPostalCode()
                )
                .country(
                        profile.getCountry()
                )
                .phone(
                        profile.getPhone()
                )
                .alternatePhone(
                        profile.getAlternatePhone()
                )
                .email(
                        profile.getEmail()
                )
                .website(
                        profile.getWebsite()
                )
                .logoConfigured(
                        profile.getLogoStorageKey() != null
                )
                .signatureConfigured(
                        profile.getSignatureStorageKey() != null
                )
                .signatureOwnerRefId(
                        profile.getSignatureOwnerRefId()
                )
                .reportFooterText(
                        profile.getReportFooterText()
                )
                .reportDisclaimer(
                        profile.getReportDisclaimer()
                )
                .version(
                        profile.getVersion()
                )
                .createdAt(
                        profile.getCreatedAt()
                )
                .updatedAt(
                        profile.getUpdatedAt()
                )
                .build();
    }
}
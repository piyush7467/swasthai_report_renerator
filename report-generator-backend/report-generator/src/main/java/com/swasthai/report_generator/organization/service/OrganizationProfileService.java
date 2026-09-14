package com.swasthai.report_generator.organization.service;

import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationProfileRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OrganizationProfileService {

    OrganizationProfileResponse getMyProfile();

    OrganizationProfileResponse updateMyProfile(
            UpdateOrganizationProfileRequest request
    );

    OrganizationProfileResponse getProfileByOrganizationRefId(
            String organizationRefId
    );

    OrganizationProfileResponse updateProfileByOrganizationRefId(
            String organizationRefId,
            UpdateOrganizationProfileRequest request
    );

    OrganizationProfileResponse uploadMyLogo(
            MultipartFile file
    );

    OrganizationProfileResponse deleteMyLogo();

    OrganizationProfileResponse uploadMySignature(
            MultipartFile file
    );

    OrganizationProfileResponse deleteMySignature();

    OrganizationProfileResponse uploadLogoForOrganization(
            String organizationRefId,
            MultipartFile file
    );

    OrganizationProfileResponse deleteLogoForOrganization(
            String organizationRefId
    );

    OrganizationProfileResponse uploadSignatureForOrganization(
            String organizationRefId,
            MultipartFile file
    );

    OrganizationProfileResponse deleteSignatureForOrganization(
            String organizationRefId
    );
}
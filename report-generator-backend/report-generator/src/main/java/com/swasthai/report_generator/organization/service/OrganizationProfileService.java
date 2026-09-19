package com.swasthai.report_generator.organization.service;

import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationProfileRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationImageResponse;
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

    OrganizationImageResponse getMyLogo();

    OrganizationProfileResponse uploadMySignature(
            MultipartFile file
    );

    OrganizationProfileResponse deleteMySignature();

    OrganizationImageResponse getMySignature();

    OrganizationProfileResponse uploadLogoForOrganization(
            String organizationRefId,
            MultipartFile file
    );

    OrganizationProfileResponse deleteLogoForOrganization(
            String organizationRefId
    );

    OrganizationImageResponse getLogoForOrganization(
            String organizationRefId
    );

    OrganizationProfileResponse uploadSignatureForOrganization(
            String organizationRefId,
            MultipartFile file
    );

    OrganizationProfileResponse deleteSignatureForOrganization(
            String organizationRefId
    );

    OrganizationImageResponse getSignatureForOrganization(
            String organizationRefId
    );
}
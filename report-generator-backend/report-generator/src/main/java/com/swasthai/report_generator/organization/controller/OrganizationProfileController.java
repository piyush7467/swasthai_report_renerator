package com.swasthai.report_generator.organization.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationProfileRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationImageResponse;
import com.swasthai.report_generator.organization.dto.response.OrganizationProfileResponse;
import com.swasthai.report_generator.organization.service.OrganizationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/organization-profile")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationProfileService profileService;

    // ============================================================
    // ORG ADMIN - OWN PROFILE
    // ============================================================

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    getMyProfile() {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization profile retrieved successfully.")
                        .data(profileService.getMyProfile())
                        .build()
        );
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    updateMyProfile(
            @Valid @RequestBody UpdateOrganizationProfileRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization profile updated successfully.")
                        .data(profileService.updateMyProfile(request))
                        .build()
        );
    }

    // ============================================================
    // ORG ADMIN - LOGO
    // ============================================================

    @PostMapping(
            value = "/me/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    uploadMyLogo(
            @RequestPart("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization logo uploaded successfully.")
                        .data(profileService.uploadMyLogo(file))
                        .build()
        );
    }

    @DeleteMapping("/me/logo")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    deleteMyLogo() {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization logo removed successfully.")
                        .data(profileService.deleteMyLogo())
                        .build()
        );
    }

    @GetMapping(value = "/me/logo")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<byte[]> getMyLogo() {

        OrganizationImageResponse image = profileService.getMyLogo();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getBytes());
    }

    // ============================================================
    // ORG ADMIN - SIGNATURE
    // ============================================================

    @PostMapping(
            value = "/me/signature",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    uploadMySignature(
            @RequestPart("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization signature uploaded successfully.")
                        .data(profileService.uploadMySignature(file))
                        .build()
        );
    }

    @DeleteMapping("/me/signature")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    deleteMySignature() {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization signature removed successfully.")
                        .data(profileService.deleteMySignature())
                        .build()
        );
    }

    @GetMapping(value = "/me/signature")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<byte[]> getMySignature() {

        OrganizationImageResponse image = profileService.getMySignature();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getBytes());
    }

    // ============================================================
    // SUPER ADMIN - PROFILE
    // ============================================================

    @GetMapping("/organizations/{organizationRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    getOrganizationProfile(
            @PathVariable String organizationRefId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization profile retrieved successfully.")
                        .data(
                                profileService.getProfileByOrganizationRefId(
                                        organizationRefId
                                )
                        )
                        .build()
        );
    }

    @PutMapping("/organizations/{organizationRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    updateOrganizationProfile(
            @PathVariable String organizationRefId,
            @Valid @RequestBody UpdateOrganizationProfileRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization profile updated successfully.")
                        .data(
                                profileService.updateProfileByOrganizationRefId(
                                        organizationRefId,
                                        request
                                )
                        )
                        .build()
        );
    }

    // ============================================================
    // SUPER ADMIN - LOGO
    // ============================================================

    @PostMapping(
            value = "/organizations/{organizationRefId}/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    uploadOrganizationLogo(
            @PathVariable String organizationRefId,
            @RequestPart("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization logo uploaded successfully.")
                        .data(
                                profileService.uploadLogoForOrganization(
                                        organizationRefId,
                                        file
                                )
                        )
                        .build()
        );
    }

    @DeleteMapping("/organizations/{organizationRefId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    deleteOrganizationLogo(
            @PathVariable String organizationRefId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization logo removed successfully.")
                        .data(
                                profileService.deleteLogoForOrganization(
                                         organizationRefId
                                )
                        )
                        .build()
        );
    }

    @GetMapping(value = "/organizations/{organizationRefId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> getOrganizationLogo(
            @PathVariable String organizationRefId
    ) {

        OrganizationImageResponse image =
                profileService.getLogoForOrganization(organizationRefId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getBytes());
    }

    // ============================================================
    // SUPER ADMIN - SIGNATURE
    // ============================================================

    @PostMapping(
            value = "/organizations/{organizationRefId}/signature",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    uploadOrganizationSignature(
            @PathVariable String organizationRefId,
            @RequestPart("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization signature uploaded successfully.")
                        .data(
                                profileService.uploadSignatureForOrganization(
                                        organizationRefId,
                                        file
                                )
                        )
                        .build()
        );
    }

    @DeleteMapping("/organizations/{organizationRefId}/signature")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>>
    deleteOrganizationSignature(
            @PathVariable String organizationRefId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<OrganizationProfileResponse>builder()
                        .success(true)
                        .message("Organization signature removed successfully.")
                        .data(
                                profileService.deleteSignatureForOrganization(
                                        organizationRefId
                                )
                        )
                        .build()
        );
    }

    @GetMapping(value = "/organizations/{organizationRefId}/signature")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> getOrganizationSignature(
            @PathVariable String organizationRefId
    ) {

        OrganizationImageResponse image =
                profileService.getSignatureForOrganization(organizationRefId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getBytes());
    }
}
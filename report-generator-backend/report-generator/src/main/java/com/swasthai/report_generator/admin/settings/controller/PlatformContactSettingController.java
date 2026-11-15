package com.swasthai.report_generator.admin.settings.controller;

import com.swasthai.report_generator.admin.settings.dto.PlatformContactSettingDto;
import com.swasthai.report_generator.admin.settings.service.PlatformContactSettingService;
import com.swasthai.report_generator.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PlatformContactSettingController {

    private final PlatformContactSettingService contactSettingService;

    /**
     * Public endpoint to get platform contact and legal info.
     * Open to all users without authentication.
     */
    @GetMapping("/api/v1/public/contact-info")
    public ApiResponse<PlatformContactSettingDto> getPublicContactInfo() {
        return ApiResponse.success(
                "Contact info retrieved successfully",
                contactSettingService.getContactSettings()
        );
    }

    /**
     * Super Admin endpoint to get platform contact and legal info.
     */
    @GetMapping("/api/v1/admin/contact-info")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<PlatformContactSettingDto> getAdminContactInfo() {
        return ApiResponse.success(
                "Contact info retrieved successfully",
                contactSettingService.getContactSettings()
        );
    }

    /**
     * Super Admin endpoint to update platform contact info.
     */
    @PutMapping("/api/v1/admin/contact-info")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<PlatformContactSettingDto> updateContactInfo(
            @Valid @RequestBody PlatformContactSettingDto dto) {
        return ApiResponse.success(
                "Contact info updated successfully",
                contactSettingService.updateContactSettings(dto)
        );
    }
}


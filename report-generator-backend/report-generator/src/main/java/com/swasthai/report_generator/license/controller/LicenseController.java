package com.swasthai.report_generator.license.controller;

import com.swasthai.report_generator.license.dto.request.ActivateLicenseRequest;
import com.swasthai.report_generator.license.dto.request.CreatePlanRequest;
import com.swasthai.report_generator.license.dto.request.RenewLicenseRequest;
import com.swasthai.report_generator.license.dto.request.UpdatePlanRequest;
import com.swasthai.report_generator.license.dto.response.LicenseResponse;
import com.swasthai.report_generator.license.dto.response.PlanResponse;
import com.swasthai.report_generator.license.service.LicenseService;
import com.swasthai.report_generator.license.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final PlanService planService;

    // ============================================================
    // PLAN ADMINISTRATION
    // ============================================================

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse createPlan(
            @Valid @RequestBody CreatePlanRequest request
    ) {

        return planService.createPlan(request);
    }

    @PutMapping("/plans/{planRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse updatePlan(
            @PathVariable String planRefId,
            @Valid @RequestBody UpdatePlanRequest request
    ) {

        return planService.updatePlan(
                planRefId,
                request
        );
    }

    @GetMapping("/plans/{planRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse getPlan(
            @PathVariable String planRefId
    ) {

        return planService.getPlan(
                planRefId
        );
    }

    @GetMapping("/plans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PlanResponse> getPlans() {

        return planService.getAllPlans();
    }

    // ============================================================
    // LICENSE ADMINISTRATION
    // ============================================================

    @PostMapping(
            "/organizations/{organizationRefId}/license"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LicenseResponse activateLicense(
            @PathVariable String organizationRefId,
            @Valid @RequestBody ActivateLicenseRequest request
    ) {

        return licenseService.activateLicense(
                organizationRefId,
                request
        );
    }

    @PostMapping(
            "/organizations/{organizationRefId}/license/renew"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LicenseResponse renewLicense(
            @PathVariable String organizationRefId,
            @Valid @RequestBody RenewLicenseRequest request
    ) {

        return licenseService.renewLicense(
                organizationRefId,
                request
        );
    }

    @GetMapping(
            "/organizations/{organizationRefId}/license"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LicenseResponse getLicense(
            @PathVariable String organizationRefId
    ) {

        return licenseService.getLicenseForOrganization(
                organizationRefId
        );
    }

    // ============================================================
    // ORGANIZATION LICENSE VIEW
    // ============================================================

    @GetMapping("/license/me")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public LicenseResponse getOwnLicense() {

        return licenseService.getOwnLicense();
    }
}
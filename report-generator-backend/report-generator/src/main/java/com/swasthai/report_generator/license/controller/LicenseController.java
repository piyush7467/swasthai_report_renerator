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
import com.swasthai.report_generator.license.dto.request.CreateUpgradeRequest;
import com.swasthai.report_generator.license.dto.request.UpdateUpgradeRequestStatusRequest;
import com.swasthai.report_generator.license.dto.response.AvailablePlanResponse;
import com.swasthai.report_generator.license.dto.response.OrganizationLicenseOverviewResponse;
import com.swasthai.report_generator.license.dto.response.PlanUpgradeRequestResponse;
import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import com.swasthai.report_generator.license.service.PlanUpgradeRequestService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final PlanService planService;
    private final PlanUpgradeRequestService planUpgradeRequestService;

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

    @PostMapping(
            "/organizations/{organizationRefId}/license/deactivate"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LicenseResponse deactivateLicense(
            @PathVariable String organizationRefId
    ) {

        return licenseService.deactivateLicense(
                organizationRefId
        );
    }

    @PostMapping(
            "/organizations/{organizationRefId}/license/reactivate"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LicenseResponse reactivateLicense(
            @PathVariable String organizationRefId
    ) {

        return licenseService.reactivateLicense(
                organizationRefId
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

    // ============================================================
    // ORGANIZATION LICENSE OVERVIEW & UPGRADE WORKFLOW (ORG_ADMIN)
    // ============================================================

    @GetMapping("/license/overview")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public OrganizationLicenseOverviewResponse getOrganizationLicenseOverview() {
        return planUpgradeRequestService.getOrganizationLicenseOverview();
    }

    @GetMapping("/license/available-plans")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public List<AvailablePlanResponse> getAvailablePlansForUpgrade() {
        return planUpgradeRequestService.getAvailablePlansForUpgrade();
    }

    @PostMapping("/license/upgrade-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public PlanUpgradeRequestResponse createUpgradeRequest(
            @Valid @RequestBody CreateUpgradeRequest request
    ) {
        return planUpgradeRequestService.createUpgradeRequest(request);
    }

    @GetMapping("/license/upgrade-requests/my")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Page<PlanUpgradeRequestResponse> getMyUpgradeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UpgradeRequestStatus status
    ) {
        return planUpgradeRequestService.getMyUpgradeRequests(page, size, status);
    }

    @GetMapping("/license/upgrade-requests/{refId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public PlanUpgradeRequestResponse getMyUpgradeRequestDetails(
            @PathVariable String refId
    ) {
        return planUpgradeRequestService.getMyUpgradeRequestDetails(refId);
    }

    @PostMapping("/license/upgrade-requests/{refId}/cancel")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public PlanUpgradeRequestResponse cancelMyUpgradeRequest(
            @PathVariable String refId
    ) {
        return planUpgradeRequestService.cancelMyUpgradeRequest(refId);
    }

    // ============================================================
    // SUPER ADMIN UPGRADE REQUEST MANAGEMENT
    // ============================================================

    @GetMapping("/admin/license/upgrade-requests")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<PlanUpgradeRequestResponse> getAllUpgradeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UpgradeRequestStatus status,
            @RequestParam(required = false) String organizationRefId
    ) {
        return planUpgradeRequestService.getAllUpgradeRequests(page, size, status, organizationRefId);
    }

    @GetMapping("/admin/license/upgrade-requests/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanUpgradeRequestResponse getAdminUpgradeRequestDetails(
            @PathVariable String refId
    ) {
        return planUpgradeRequestService.getUpgradeRequestDetails(refId);
    }

    @PatchMapping("/admin/license/upgrade-requests/{refId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanUpgradeRequestResponse updateUpgradeRequestStatus(
            @PathVariable String refId,
            @Valid @RequestBody UpdateUpgradeRequestStatusRequest request
    ) {
        return planUpgradeRequestService.updateUpgradeRequestStatus(refId, request);
    }
}
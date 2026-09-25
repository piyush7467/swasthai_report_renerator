package com.swasthai.report_generator.license.service;

import com.swasthai.report_generator.license.dto.request.CreateUpgradeRequest;
import com.swasthai.report_generator.license.dto.request.UpdateUpgradeRequestStatusRequest;
import com.swasthai.report_generator.license.dto.response.AvailablePlanResponse;
import com.swasthai.report_generator.license.dto.response.OrganizationLicenseOverviewResponse;
import com.swasthai.report_generator.license.dto.response.PlanUpgradeRequestResponse;
import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PlanUpgradeRequestService {

    OrganizationLicenseOverviewResponse getOrganizationLicenseOverview();

    List<AvailablePlanResponse> getAvailablePlansForUpgrade();

    PlanUpgradeRequestResponse createUpgradeRequest(CreateUpgradeRequest request);

    Page<PlanUpgradeRequestResponse> getMyUpgradeRequests(int page, int size, UpgradeRequestStatus status);

    PlanUpgradeRequestResponse getMyUpgradeRequestDetails(String refId);

    PlanUpgradeRequestResponse cancelMyUpgradeRequest(String refId);

    Page<PlanUpgradeRequestResponse> getAllUpgradeRequests(int page, int size, UpgradeRequestStatus status, String organizationRefId);

    PlanUpgradeRequestResponse getUpgradeRequestDetails(String refId);

    PlanUpgradeRequestResponse updateUpgradeRequestStatus(String refId, UpdateUpgradeRequestStatusRequest request);
}

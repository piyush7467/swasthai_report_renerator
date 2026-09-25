package com.swasthai.report_generator.user.service;

import com.swasthai.report_generator.user.dto.request.CreateLabStaffRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.LabStaffDetailsResponse;
import com.swasthai.report_generator.user.dto.response.LabStaffSummaryResponse;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.UserStatus;

public interface LabStaffService {

    LabStaffSummaryResponse getLabStaffSummary();

    UserPageResponse getLabStaffList(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            UserStatus status,
            String search
    );

    LabStaffDetailsResponse getLabStaffDetails(String refId);

    UserResponse createLabStaff(CreateLabStaffRequest request);

    UserResponse updateLabStaffStatus(String refId, UpdateUserStatusRequest request);

    UserResponse deactivateLabStaff(String refId);

    void deleteLabStaff(String refId);
}

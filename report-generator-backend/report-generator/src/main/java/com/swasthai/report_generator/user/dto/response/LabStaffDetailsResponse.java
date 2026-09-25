package com.swasthai.report_generator.user.dto.response;

import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabStaffDetailsResponse {

    private String refId;
    private String name;
    private String email;
    private Role role;
    private UserStatus status;
    private String organizationRefId;
    private String organizationName;
    private Instant lastLoginAt;
    private Instant inactiveAt;
    private Instant eligibleForCleanupAt;
    private Boolean cleanupEligible;
    private String deactivatedByName;
    private Instant createdAt;
    private Instant updatedAt;
    private long reportsCreated;
    private long reportsFinalized;
}

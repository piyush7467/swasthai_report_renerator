package com.swasthai.report_generator.user.dto.response;

import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserResponse {

    private String refId;

    private String name;

    private String email;

    private Role role;

    private UserStatus status;

    private String organizationRefId;

    private Instant lastLoginAt;

    private Instant createdAt;

    private Instant updatedAt;
}
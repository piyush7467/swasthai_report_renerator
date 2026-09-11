package com.swasthai.report_generator.user.service;

import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.UserStatus;

public interface UserService {

    UserResponse createUser(
            CreateUserRequest request
    );

    UserPageResponse getUsers(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Role role,
            UserStatus status
    );

    UserResponse getUserByRefId(
            String refId
    );

    UserResponse updateUser(
            String refId,
            UpdateUserRequest request
    );

    UserResponse updateUserStatus(
            String refId,
            UpdateUserStatusRequest request
    );
}
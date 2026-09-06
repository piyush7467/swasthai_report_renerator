package com.swasthai.report_generator.user.service;

import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

}
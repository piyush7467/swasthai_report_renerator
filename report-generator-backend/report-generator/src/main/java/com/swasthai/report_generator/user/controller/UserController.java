package com.swasthai.report_generator.user.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response =
                userService.createUser(request);

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User created successfully.")
                        .data(response)
                        .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponse);
    }
}
package com.swasthai.report_generator.user.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.UserStatus;
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

    // ============================================================
    // CREATE
    // ============================================================

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

    // ============================================================
    // LIST
    // ============================================================

    @GetMapping
    public ResponseEntity<ApiResponse<UserPageResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status
    ) {

        UserPageResponse response =
                userService.getUsers(
                        page,
                        size,
                        sortBy,
                        sortDirection,
                        role,
                        status
                );

        ApiResponse<UserPageResponse> apiResponse =
                ApiResponse.<UserPageResponse>builder()
                        .success(true)
                        .message("Users retrieved successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // GET BY REF ID
    // ============================================================

    @GetMapping("/{refId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String refId
    ) {

        UserResponse response =
                userService.getUserByRefId(refId);

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User retrieved successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @PutMapping("/{refId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String refId,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResponse response =
                userService.updateUser(
                        refId,
                        request
                );

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User updated successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // STATUS
    // ============================================================

    @PatchMapping("/{refId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable String refId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {

        UserResponse response =
                userService.updateUserStatus(
                        refId,
                        request
                );

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User status updated successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }
}
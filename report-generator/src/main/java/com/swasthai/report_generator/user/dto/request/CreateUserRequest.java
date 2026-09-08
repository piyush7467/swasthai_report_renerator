package com.swasthai.report_generator.user.dto.request;

import com.swasthai.report_generator.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class CreateUserRequest {

    @NotBlank(message = "User name is required")
    @Size(max = 150, message = "User name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    /*
     * Required for ORG_ADMIN and LAB_STAFF.
     *
     * SUPER_ADMIN must not have an organization.
     */
    private String organizationRefId;
}
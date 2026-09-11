package com.swasthai.report_generator.user.dto.request;


import com.swasthai.report_generator.user.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "User name is required")
    @Size(
            max = 150,
            message = "User name must not exceed 150 characters"
    )
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(
            max = 150,
            message = "Email must not exceed 150 characters"
    )
    private String email;

    /*
     * Only SUPER_ADMIN may change the role.
     *
     * ORG_ADMIN must leave this null.
     */
    private Role role;

    /*
     * Only SUPER_ADMIN may change organization.
     *
     * Required when SUPER_ADMIN is changing/assigning
     * an organization-bound user.
     */
    private String organizationRefId;
}
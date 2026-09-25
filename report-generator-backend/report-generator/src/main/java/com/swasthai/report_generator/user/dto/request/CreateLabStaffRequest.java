package com.swasthai.report_generator.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabStaffRequest {

    @NotBlank(message = "Staff full name is required.")
    @Size(max = 150, message = "Staff name must not exceed 150 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email address.")
    @Size(max = 150, message = "Email must not exceed 150 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    private String password;

    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;
}

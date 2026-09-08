package com.swasthai.report_generator.patient.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class CreatePatientRequest {

    @NotBlank(message = "Patient name is required")
    @Size(max = 150, message = "Patient name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private com.swasthai.report_generator.patient.entity.Gender gender;

    @jakarta.validation.constraints.Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must be valid (7 to 15 digits, optional + prefix)"
    )
    private String phone;

    @Email(message = "Invalid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}
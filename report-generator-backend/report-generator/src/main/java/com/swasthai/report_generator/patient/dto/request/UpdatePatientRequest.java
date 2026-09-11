package com.swasthai.report_generator.patient.dto.request;

import com.swasthai.report_generator.patient.entity.AgeUnit;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Salutation;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePatientRequest {

    @NotNull(message = "Salutation is required")
    private Salutation salutation;

    @NotBlank(message = "Patient name is required")
    @Size(
            max = 150,
            message = "Patient name must not exceed 150 characters"
    )
    private String name;

    @NotNull(message = "Date of birth known flag is required")
    private Boolean dateOfBirthKnown;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Positive(message = "Age must be greater than zero")
    @Max(
            value = 150,
            message = "Age value is too large"
    )
    private Integer ageValue;

    private AgeUnit ageUnit;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must be valid (7 to 15 digits, optional + prefix)"
    )
    private String phone;

    @Email(message = "Invalid email address")
    @Size(
            max = 150,
            message = "Email must not exceed 150 characters"
    )
    private String email;

    @Size(
            max = 500,
            message = "Address must not exceed 500 characters"
    )
    private String address;

    @DecimalMin(
            value = "0.001",
            message = "Weight must be greater than zero"
    )
    @DecimalMax(
            value = "999.999",
            message = "Weight is too large"
    )
    private BigDecimal weightKg;
}
package com.swasthai.report_generator.organization.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationProfileRequest {

    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String country;

    @Size(max = 30)
    @Pattern(
            regexp = "^[0-9+()\\- .]*$",
            message = "Invalid phone number format"
    )
    private String phone;

    @Size(max = 30)
    @Pattern(
            regexp = "^[0-9+()\\- .]*$",
            message = "Invalid alternate phone number format"
    )
    private String alternatePhone;

    @Email(message = "Invalid organization email address")
    @Size(max = 150)
    private String email;

    @Size(max = 255)
    @Pattern(
            regexp = "^(https?://).*$",
            message = "Website must start with http:// or https://"
    )
    private String website;

    @Size(max = 1000)
    private String reportFooterText;

    @Size(max = 2000)
    private String reportDisclaimer;
}